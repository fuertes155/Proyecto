package com.cooperativa.met.application.investment.usecase;

import com.cooperativa.met.application.investment.service.GuaranteeFundService;
import com.cooperativa.met.domain.account.model.CoreAccount;
import com.cooperativa.met.domain.account.port.CoreAccountRepositoryPort;
import com.cooperativa.met.domain.investment.model.InvestmentFraction;
import com.cooperativa.met.domain.investment.model.InvestmentMatch;
import com.cooperativa.met.domain.investment.model.LedgerEntry;
import com.cooperativa.met.domain.investment.model.LedgerEntryCategory;
import com.cooperativa.met.domain.investment.model.LedgerEntryType;
import com.cooperativa.met.domain.investment.port.InvestmentFractionRepositoryPort;
import com.cooperativa.met.domain.investment.port.InvestmentMatchRepositoryPort;
import com.cooperativa.met.domain.investment.port.LedgerRepositoryPort;
import com.cooperativa.met.domain.lending.model.AmortizationInstallment;
import com.cooperativa.met.domain.lending.model.PersonalLoanApplication;
import com.cooperativa.met.infrastructure.config.CapitalEngineProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Activa el Fondo de Garantías/Aval Colectivo cuando una cuota entra en mora
 * sostenida. Cubre parcialmente (nunca al 100%, ver {@code guaranteeFundCoverageRatio})
 * la exposición de los inversionistas que fondearon ese préstamo — el capital
 * que no alcance a cubrir el fondo queda como pérdida real del inversionista,
 * tal como se le informa en el contrato.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GuaranteeFundCompensationUseCase {

    private final InvestmentMatchRepositoryPort matchPort;
    private final InvestmentFractionRepositoryPort fractionPort;
    private final CoreAccountRepositoryPort accountPort;
    private final LedgerRepositoryPort ledgerPort;
    private final GuaranteeFundService guaranteeFundService;
    private final CapitalEngineProperties capitalEngineProperties;

    @Transactional
    public void compensate(PersonalLoanApplication loan, AmortizationInstallment lateInstallment) {
        List<InvestmentMatch> loanMatches = matchPort.findByBorrowerLoanId(loan.getId());
        if (loanMatches.isEmpty()) {
            return;
        }

        Map<UUID, BigDecimal> fundedByInvestor = new LinkedHashMap<>();
        BigDecimal totalFunded = BigDecimal.ZERO;
        for (InvestmentMatch match : loanMatches) {
            InvestmentFraction fraction = fractionPort.findById(match.getFractionId()).orElseThrow();
            fundedByInvestor.merge(fraction.getInvestorAccountId(), fraction.getAmount(), BigDecimal::add);
            totalFunded = totalFunded.add(fraction.getAmount());
        }
        if (totalFunded.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        BigDecimal owedAmount = lateInstallment.getPrincipalAmount().add(lateInstallment.getInterestAmount());
        String loanRef = loan.getId().toString().substring(0, 8);

        BigDecimal covered = guaranteeFundService.coverDefault(
                owedAmount,
                capitalEngineProperties.getGuaranteeFundCoverageRatio(),
                loan.getId(),
                "Cobertura de mora - Cuota " + lateInstallment.getInstallmentNumber() + " - Préstamo " + loanRef);

        if (covered.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Préstamo {} en mora sostenida sin cobertura del Fondo de Garantías (saldo insuficiente).", loan.getId());
            return;
        }

        List<LedgerEntry> ledgerEntries = new ArrayList<>();
        BigDecimal accumulated = BigDecimal.ZERO;
        int processed = 0;
        int total = fundedByInvestor.size();

        for (Map.Entry<UUID, BigDecimal> entry : fundedByInvestor.entrySet()) {
            processed++;
            UUID investorAccountId = entry.getKey();
            BigDecimal fundedAmount = entry.getValue();

            BigDecimal share;
            if (processed == total) {
                share = covered.subtract(accumulated);
            } else {
                BigDecimal proportion = fundedAmount.divide(totalFunded, 10, RoundingMode.HALF_UP);
                share = covered.multiply(proportion).setScale(2, RoundingMode.HALF_UP);
                accumulated = accumulated.add(share);
            }
            if (share.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            CoreAccount investorAccount = accountPort.findById(investorAccountId)
                    .orElseThrow(() -> new IllegalStateException("Cuenta de inversionista no encontrada: " + investorAccountId));
            accountPort.save(investorAccount.creditPrincipal(share));

            ledgerEntries.add(LedgerEntry.builder()
                    .id(UUID.randomUUID())
                    .accountId(investorAccountId)
                    .transactionReference(loan.getId())
                    .entryType(LedgerEntryType.CREDIT)
                    .category(LedgerEntryCategory.GUARANTEE_FUND_PAYOUT)
                    .amount(share)
                    .concept("Cobertura Fondo de Garantías - Cuota " + lateInstallment.getInstallmentNumber() + " - Préstamo " + loanRef)
                    .createdAt(Instant.now())
                    .build());
        }

        ledgerPort.saveAll(ledgerEntries);
        log.info("Fondo de Garantías cubrió {} de {} adeudado en préstamo {} (cuota {}), repartido entre {} inversionista(s).",
                covered, owedAmount, loan.getId(), lateInstallment.getInstallmentNumber(), total);
    }
}
