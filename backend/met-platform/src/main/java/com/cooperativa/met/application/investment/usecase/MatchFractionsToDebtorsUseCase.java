package com.cooperativa.met.application.investment.usecase;

import com.cooperativa.met.domain.account.model.CoreAccount;
import com.cooperativa.met.domain.account.port.CoreAccountRepositoryPort;
import com.cooperativa.met.domain.investment.model.InvestmentFraction;
import com.cooperativa.met.domain.investment.model.InvestmentFractionStatus;
import com.cooperativa.met.domain.investment.model.InvestmentMatch;
import com.cooperativa.met.domain.investment.model.LedgerEntry;
import com.cooperativa.met.domain.investment.model.LedgerEntryType;
import com.cooperativa.met.domain.investment.port.InvestmentFractionRepositoryPort;
import com.cooperativa.met.domain.investment.port.InvestmentMatchRepositoryPort;
import com.cooperativa.met.domain.investment.port.LedgerRepositoryPort;
import com.cooperativa.met.domain.lending.model.LoanApplicationStatus;
import com.cooperativa.met.domain.lending.model.PersonalLoanApplication;
import com.cooperativa.met.domain.lending.port.PersonalLoanApplicationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchFractionsToDebtorsUseCase {

    private final PersonalLoanApplicationPort loanPort;
    private final InvestmentFractionRepositoryPort fractionPort;
    private final InvestmentMatchRepositoryPort matchPort;
    private final LedgerRepositoryPort ledgerPort;
    private final CoreAccountRepositoryPort accountPort;

    @Transactional
    public void execute(List<InvestmentFraction> newFractions) {
        log.info("Iniciando motor de emparejamiento (Matching Engine) para {} fracciones nuevas.", newFractions.size());

        // Obtener todos los deudores aprobados ordenados por FIFO
        List<PersonalLoanApplication> approvedLoans = loanPort.findByStatus(LoanApplicationStatus.APPROVED);
        if (approvedLoans.isEmpty()) {
            log.info("No hay deudores aprobados en cola. Las fracciones quedarán en estado AVAILABLE.");
            return;
        }

        List<InvestmentFraction> updatedFractions = new ArrayList<>();
        List<InvestmentMatch> matches = new ArrayList<>();
        List<LedgerEntry> ledgerEntries = new ArrayList<>();

        int fractionIndex = 0;

        // Iterar sobre los deudores aprobados
        for (PersonalLoanApplication loan : approvedLoans) {
            
            // Buscar la cuenta del deudor para las entradas contables
            CoreAccount borrowerAccount = accountPort.findByUserId(loan.getUserId()).orElse(null);
            if (borrowerAccount == null) {
                log.warn("El deudor del préstamo {} no tiene cuenta asociada. Saltando emparejamiento.", loan.getId());
                continue;
            }

            // Calcular cuánto le falta por fondear a este préstamo
            // Para simplificar, calculamos según los matches actuales
            List<InvestmentMatch> currentMatches = matchPort.findByBorrowerLoanId(loan.getId());
            BigDecimal alreadyFunded = BigDecimal.ZERO;
            for (InvestmentMatch m : currentMatches) {
                InvestmentFraction f = fractionPort.findById(m.getFractionId()).orElseThrow();
                alreadyFunded = alreadyFunded.add(f.getAmount());
            }

            BigDecimal remainingNeeded = loan.getAmount().subtract(alreadyFunded);

            // Asignar fracciones a este préstamo mientras necesite fondos
            while (remainingNeeded.compareTo(BigDecimal.ZERO) > 0 && fractionIndex < newFractions.size()) {
                InvestmentFraction fraction = newFractions.get(fractionIndex);
                
                // Marcar fracción como MATCHED
                InvestmentFraction matchedFraction = fraction.withStatus(InvestmentFractionStatus.MATCHED);
                updatedFractions.add(matchedFraction);

                // Crear el Match
                InvestmentMatch match = InvestmentMatch.builder()
                        .id(UUID.randomUUID())
                        .fractionId(matchedFraction.getId())
                        .borrowerLoanId(loan.getId())
                        .matchedAt(Instant.now())
                        .build();
                matches.add(match);

                // Libro Contable: DÉBITO a cuenta ahorrador (origen de los fondos virtuales)
                ledgerEntries.add(LedgerEntry.builder()
                        .id(UUID.randomUUID())
                        .accountId(matchedFraction.getInvestorAccountId())
                        .transactionReference(match.getId())
                        .entryType(LedgerEntryType.DEBIT)
                        .amount(matchedFraction.getAmount())
                        .concept("Dispersión de inversión - Préstamo " + loan.getId().toString().substring(0,8))
                        .createdAt(Instant.now())
                        .build());

                // Libro Contable: CRÉDITO a cuenta deudor (destino de los fondos virtuales)
                ledgerEntries.add(LedgerEntry.builder()
                        .id(UUID.randomUUID())
                        .accountId(borrowerAccount.getId())
                        .transactionReference(match.getId())
                        .entryType(LedgerEntryType.CREDIT)
                        .amount(matchedFraction.getAmount())
                        .concept("Recepción de fondeo - Préstamo " + loan.getId().toString().substring(0,8))
                        .createdAt(Instant.now())
                        .build());
                
                remainingNeeded = remainingNeeded.subtract(matchedFraction.getAmount());
                fractionIndex++;
            }

            if (fractionIndex >= newFractions.size()) {
                break; // No hay más fracciones disponibles
            }
        }

        if (!updatedFractions.isEmpty()) {
            fractionPort.saveAll(updatedFractions);
            matchPort.saveAll(matches);
            ledgerPort.saveAll(ledgerEntries);
            log.info("Emparejamiento exitoso: {} matches creados, {} entradas contables.", matches.size(), ledgerEntries.size());
        }
    }
}
