package com.cooperativa.met.application.investment.usecase;

import com.cooperativa.met.domain.account.model.CoreAccount;
import com.cooperativa.met.domain.account.port.CoreAccountRepositoryPort;
import com.cooperativa.met.domain.investment.model.InvestmentFraction;
import com.cooperativa.met.domain.investment.model.InvestmentFractionStatus;
import com.cooperativa.met.domain.investment.model.InvestmentMatch;
import com.cooperativa.met.domain.investment.model.LedgerEntry;
import com.cooperativa.met.domain.investment.model.LedgerEntryCategory;
import com.cooperativa.met.domain.investment.model.LedgerEntryType;
import com.cooperativa.met.domain.investment.port.InvestmentFractionRepositoryPort;
import com.cooperativa.met.domain.investment.port.InvestmentMatchRepositoryPort;
import com.cooperativa.met.domain.investment.port.LedgerRepositoryPort;
import com.cooperativa.met.domain.lending.model.LoanApplicationStatus;
import com.cooperativa.met.domain.lending.model.PersonalLoanApplication;
import com.cooperativa.met.domain.lending.port.PersonalLoanApplicationPort;
import com.cooperativa.met.infrastructure.config.CapitalEngineProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Motor de emparejamiento (Matching Engine): asigna fracciones de inversión a
 * deudores aprobados, en orden FIFO, respetando dos reglas de integridad:
 *
 * 1. Una fracción puede DIVIDIRSE entre varios deudores — nunca se le asigna
 *    a un préstamo más de lo que realmente necesita (evita sobre-fondeo).
 * 2. Tope de concentración: un mismo depósito no puede financiar más de
 *    {@code met.capital-engine.concentration-cap-percentage} de un solo deudor
 *    (diversificación de riesgo real, no solo nominal).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MatchFractionsToDebtorsUseCase {

    private final PersonalLoanApplicationPort loanPort;
    private final InvestmentFractionRepositoryPort fractionPort;
    private final InvestmentMatchRepositoryPort matchPort;
    private final LedgerRepositoryPort ledgerPort;
    private final CoreAccountRepositoryPort accountPort;
    private final CapitalEngineProperties capitalEngineProperties;

    private record Allocation(PersonalLoanApplication loan, BigDecimal amount) {}

    @Transactional
    public void execute(List<InvestmentFraction> newFractions) {
        log.info("Iniciando motor de emparejamiento (Matching Engine) para {} fracciones nuevas.", newFractions.size());

        List<PersonalLoanApplication> approvedLoans = loanPort.findByStatus(LoanApplicationStatus.APPROVED);
        if (approvedLoans.isEmpty()) {
            log.info("No hay deudores aprobados en cola. Las fracciones quedarán en estado AVAILABLE.");
            return;
        }

        Map<UUID, CoreAccount> borrowerAccounts = new HashMap<>();
        Map<UUID, BigDecimal> remainingNeedByLoan = new LinkedHashMap<>();
        Map<String, BigDecimal> concentrationFunded = new HashMap<>();

        for (PersonalLoanApplication loan : approvedLoans) {
            CoreAccount borrowerAccount = accountPort.findByUserId(loan.getUserId()).orElse(null);
            if (borrowerAccount == null) {
                log.warn("El deudor del préstamo {} no tiene cuenta asociada. Saltando emparejamiento.", loan.getId());
                continue;
            }
            borrowerAccounts.put(loan.getId(), borrowerAccount);

            BigDecimal alreadyFunded = BigDecimal.ZERO;
            for (InvestmentMatch match : matchPort.findByBorrowerLoanId(loan.getId())) {
                InvestmentFraction fundedFraction = fractionPort.findById(match.getFractionId()).orElseThrow();
                alreadyFunded = alreadyFunded.add(fundedFraction.getAmount());
                concentrationFunded.merge(
                        concentrationKey(fundedFraction.getOriginalDepositId(), loan.getId()),
                        fundedFraction.getAmount(), BigDecimal::add);
            }
            remainingNeedByLoan.put(loan.getId(), loan.getAmount().subtract(alreadyFunded));
        }

        BigDecimal concentrationCapPct = capitalEngineProperties.getConcentrationCapPercentage();
        Map<UUID, BigDecimal> totalDepositCache = new HashMap<>();

        List<InvestmentFraction> fractionsToSave = new ArrayList<>();
        List<InvestmentMatch> matches = new ArrayList<>();
        List<LedgerEntry> ledgerEntries = new ArrayList<>();

        for (InvestmentFraction original : newFractions) {
            BigDecimal totalDeposit = totalDepositCache.computeIfAbsent(
                    original.getOriginalDepositId(), this::sumFractionsForDeposit);
            BigDecimal concentrationCap = totalDeposit.multiply(concentrationCapPct).setScale(2, RoundingMode.HALF_UP);

            BigDecimal remaining = original.getAmount();
            List<Allocation> allocations = new ArrayList<>();

            for (PersonalLoanApplication loan : approvedLoans) {
                if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }
                if (!borrowerAccounts.containsKey(loan.getId())) {
                    continue;
                }

                BigDecimal need = remainingNeedByLoan.getOrDefault(loan.getId(), BigDecimal.ZERO);
                if (need.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                String key = concentrationKey(original.getOriginalDepositId(), loan.getId());
                BigDecimal capRoom = concentrationCap.subtract(concentrationFunded.getOrDefault(key, BigDecimal.ZERO));
                if (capRoom.compareTo(BigDecimal.ZERO) <= 0) {
                    continue; // este depósito ya alcanzó su tope de concentración en este deudor
                }

                BigDecimal allocate = remaining.min(need).min(capRoom);
                if (allocate.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                allocations.add(new Allocation(loan, allocate));
                remainingNeedByLoan.merge(loan.getId(), allocate.negate(), BigDecimal::add);
                concentrationFunded.merge(key, allocate, BigDecimal::add);
                remaining = remaining.subtract(allocate);
            }

            if (allocations.isEmpty()) {
                continue; // sin cupo en ningún deudor (o tope de concentración copado); sigue AVAILABLE tal cual está en BD
            }

            boolean singleFullAllocation = allocations.size() == 1
                    && allocations.get(0).amount().compareTo(original.getAmount()) == 0;

            if (singleFullAllocation) {
                Allocation allocation = allocations.get(0);
                InvestmentFraction matchedFraction = original.withStatus(InvestmentFractionStatus.MATCHED);
                fractionsToSave.add(matchedFraction);
                addMatchAndLedger(matchedFraction, allocation, borrowerAccounts.get(allocation.loan().getId()),
                        matches, ledgerEntries);
            } else {
                fractionsToSave.add(original.withStatus(InvestmentFractionStatus.SPLIT));
                for (Allocation allocation : allocations) {
                    InvestmentFraction piece = InvestmentFraction.builder()
                            .id(UUID.randomUUID())
                            .investorAccountId(original.getInvestorAccountId())
                            .originalDepositId(original.getOriginalDepositId())
                            .amount(allocation.amount())
                            .status(InvestmentFractionStatus.MATCHED)
                            .createdAt(original.getCreatedAt())
                            .matchedAt(Instant.now())
                            .build();
                    fractionsToSave.add(piece);
                    addMatchAndLedger(piece, allocation, borrowerAccounts.get(allocation.loan().getId()),
                            matches, ledgerEntries);
                }
                if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                    fractionsToSave.add(InvestmentFraction.builder()
                            .id(UUID.randomUUID())
                            .investorAccountId(original.getInvestorAccountId())
                            .originalDepositId(original.getOriginalDepositId())
                            .amount(remaining)
                            .status(InvestmentFractionStatus.AVAILABLE)
                            .createdAt(Instant.now())
                            .build());
                }
            }
        }

        if (!fractionsToSave.isEmpty()) {
            fractionPort.saveAll(fractionsToSave);
            matchPort.saveAll(matches);
            ledgerPort.saveAll(ledgerEntries);
            log.info("Emparejamiento: {} matches creados, {} entradas contables, {} filas de fracción actualizadas/creadas.",
                    matches.size(), ledgerEntries.size(), fractionsToSave.size());
        } else {
            log.info("Ninguna fracción pudo asignarse (sin cupo de deudores o tope de concentración alcanzado). Quedan AVAILABLE.");
        }
    }

    private void addMatchAndLedger(InvestmentFraction fraction, Allocation allocation, CoreAccount borrowerAccount,
                                    List<InvestmentMatch> matches, List<LedgerEntry> ledgerEntries) {
        InvestmentMatch match = InvestmentMatch.builder()
                .id(UUID.randomUUID())
                .fractionId(fraction.getId())
                .borrowerLoanId(allocation.loan().getId())
                .matchedAt(Instant.now())
                .build();
        matches.add(match);

        String loanRef = allocation.loan().getId().toString().substring(0, 8);

        ledgerEntries.add(LedgerEntry.builder()
                .id(UUID.randomUUID())
                .accountId(fraction.getInvestorAccountId())
                .transactionReference(match.getId())
                .entryType(LedgerEntryType.DEBIT)
                .category(LedgerEntryCategory.FUNDING_DISBURSEMENT)
                .amount(allocation.amount())
                .concept("Dispersión de inversión - Préstamo " + loanRef)
                .createdAt(Instant.now())
                .build());

        ledgerEntries.add(LedgerEntry.builder()
                .id(UUID.randomUUID())
                .accountId(borrowerAccount.getId())
                .transactionReference(match.getId())
                .entryType(LedgerEntryType.CREDIT)
                .category(LedgerEntryCategory.FUNDING_DISBURSEMENT)
                .amount(allocation.amount())
                .concept("Recepción de fondeo - Préstamo " + loanRef)
                .createdAt(Instant.now())
                .build());
    }

    private BigDecimal sumFractionsForDeposit(UUID depositId) {
        return fractionPort.findByOriginalDepositId(depositId).stream()
                .map(InvestmentFraction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String concentrationKey(UUID depositId, UUID loanId) {
        return depositId + "|" + loanId;
    }
}
