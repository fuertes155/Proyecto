package com.cooperativa.met.application.investment.usecase;

import com.cooperativa.met.domain.account.model.CoreAccount;
import com.cooperativa.met.domain.account.port.CoreAccountRepositoryPort;
import com.cooperativa.met.domain.admin.model.PlatformRevenue;
import com.cooperativa.met.domain.admin.port.PlatformRevenuePort;
import com.cooperativa.met.domain.investment.model.InvestmentFraction;
import com.cooperativa.met.domain.investment.model.InvestmentMatch;
import com.cooperativa.met.domain.investment.model.InvestmentReturn;
import com.cooperativa.met.domain.investment.model.LedgerEntry;
import com.cooperativa.met.domain.investment.model.LedgerEntryCategory;
import com.cooperativa.met.domain.investment.model.LedgerEntryType;
import com.cooperativa.met.domain.investment.port.InvestmentFractionRepositoryPort;
import com.cooperativa.met.domain.investment.port.InvestmentMatchRepositoryPort;
import com.cooperativa.met.domain.investment.port.InvestmentReturnPort;
import com.cooperativa.met.domain.investment.port.LedgerRepositoryPort;
import com.cooperativa.met.domain.investment.service.margin.MarginCalculationResult;
import com.cooperativa.met.domain.investment.service.margin.MarginStrategy;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Waterfall de distribución de un pago de cuota. Se dispara cuando un cobro
 * de {@link com.cooperativa.met.application.lending.usecase.ProcessLoanCollectionsUseCase}
 * fue exitoso. Orden de la cascada:
 *
 * 1. Capital de la cuota → se acredita, proporcional al % de fondeo, a cada
 *    inversionista que fondeó ese préstamo (vía {@link InvestmentMatch}).
 * 2. Interés de la cuota → pasa por el {@link MarginStrategy} configurado:
 *    Comisión_plataforma (queda en la plataforma, registrada en {@link PlatformRevenue})
 *    y Rendimiento_neto (se acredita a los inversionistas, proporcional).
 *
 * El Fondo de Garantías NO participa en el camino feliz: se alimenta aparte
 * (tarifa de desembolso) y solo se activa en mora — ver {@link GuaranteeFundCompensationUseCase}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentDistributionUseCase {

    private final InvestmentMatchRepositoryPort matchPort;
    private final InvestmentFractionRepositoryPort fractionPort;
    private final CoreAccountRepositoryPort accountPort;
    private final LedgerRepositoryPort ledgerPort;
    private final InvestmentReturnPort investmentReturnPort;
    private final PlatformRevenuePort platformRevenuePort;
    private final CapitalEngineProperties capitalEngineProperties;

    @Transactional
    public void distribute(PersonalLoanApplication loan, AmortizationInstallment paidInstallment) {
        List<InvestmentMatch> loanMatches = matchPort.findByBorrowerLoanId(loan.getId());
        if (loanMatches.isEmpty()) {
            log.warn("Préstamo {} pagó la cuota {} pero no tiene inversionistas emparejados. No hay nada que distribuir.",
                    loan.getId(), paidInstallment.getInstallmentNumber());
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
            log.error("Préstamo {} tiene matches con fondeo total cero. Se aborta la distribución.", loan.getId());
            return;
        }

        MarginStrategy strategy = MarginStrategy.of(capitalEngineProperties.getMarginModel());
        MarginCalculationResult margin = strategy.calculate(
                loan.getAmount(), paidInstallment.getInterestAmount(), capitalEngineProperties.getMarginRate());

        UUID paymentReference = paidInstallment.getId() != null ? paidInstallment.getId() : UUID.randomUUID();
        String loanRef = loan.getId().toString().substring(0, 8);
        List<LedgerEntry> ledgerEntries = new ArrayList<>();

        BigDecimal principalAccum = BigDecimal.ZERO;
        BigDecimal yieldAccum = BigDecimal.ZERO;
        int processed = 0;
        int total = fundedByInvestor.size();

        for (Map.Entry<UUID, BigDecimal> entry : fundedByInvestor.entrySet()) {
            processed++;
            UUID investorAccountId = entry.getKey();
            BigDecimal fundedAmount = entry.getValue();

            BigDecimal principalShare;
            BigDecimal yieldShare;
            if (processed == total) {
                // el último inversionista recibe el residuo, evitando pérdidas de centavos por redondeo
                principalShare = paidInstallment.getPrincipalAmount().subtract(principalAccum);
                yieldShare = margin.getInvestorYield().subtract(yieldAccum);
            } else {
                BigDecimal proportion = fundedAmount.divide(totalFunded, 10, RoundingMode.HALF_UP);
                principalShare = paidInstallment.getPrincipalAmount().multiply(proportion).setScale(2, RoundingMode.HALF_UP);
                yieldShare = margin.getInvestorYield().multiply(proportion).setScale(2, RoundingMode.HALF_UP);
                principalAccum = principalAccum.add(principalShare);
                yieldAccum = yieldAccum.add(yieldShare);
            }

            CoreAccount investorAccount = accountPort.findById(investorAccountId)
                    .orElseThrow(() -> new IllegalStateException("Cuenta de inversionista no encontrada: " + investorAccountId));

            CoreAccount updatedAccount = investorAccount;
            if (principalShare.compareTo(BigDecimal.ZERO) > 0) {
                updatedAccount = updatedAccount.creditPrincipal(principalShare);
                ledgerEntries.add(LedgerEntry.builder()
                        .id(UUID.randomUUID())
                        .accountId(investorAccountId)
                        .transactionReference(paymentReference)
                        .entryType(LedgerEntryType.CREDIT)
                        .category(LedgerEntryCategory.PRINCIPAL_REPAYMENT)
                        .amount(principalShare)
                        .concept("Abono a capital - Cuota " + paidInstallment.getInstallmentNumber() + " - Préstamo " + loanRef)
                        .createdAt(Instant.now())
                        .build());
            }
            if (yieldShare.compareTo(BigDecimal.ZERO) > 0) {
                updatedAccount = updatedAccount.creditInterest(yieldShare);
                ledgerEntries.add(LedgerEntry.builder()
                        .id(UUID.randomUUID())
                        .accountId(investorAccountId)
                        .transactionReference(paymentReference)
                        .entryType(LedgerEntryType.CREDIT)
                        .category(LedgerEntryCategory.INTEREST_YIELD)
                        .amount(yieldShare)
                        .concept("Rendimiento neto (" + margin.getModel() + ") - Cuota " + paidInstallment.getInstallmentNumber()
                                + " - Préstamo " + loanRef)
                        .createdAt(Instant.now())
                        .build());
            }
            accountPort.save(updatedAccount);

            investmentReturnPort.save(InvestmentReturn.builder()
                    .id(UUID.randomUUID())
                    .investmentId(loan.getId())
                    .userId(investorAccount.getUserId())
                    .capital(principalShare)
                    .rendimiento(yieldShare)
                    .totalAcreditado(principalShare.add(yieldShare))
                    .fechaPago(LocalDate.now())
                    .createdAt(Instant.now())
                    .build());
        }

        if (margin.getPlatformCommission().compareTo(BigDecimal.ZERO) > 0) {
            platformRevenuePort.save(PlatformRevenue.builder()
                    .id(UUID.randomUUID())
                    .userId(loan.getUserId())
                    .amount(margin.getPlatformCommission())
                    .description("Comisión de plataforma (" + margin.getModel() + ") - Cuota "
                            + paidInstallment.getInstallmentNumber() + " - Préstamo " + loanRef)
                    .source("LOAN_INSTALLMENT_MARGIN")
                    .createdAt(Instant.now())
                    .build());
        }

        if (!ledgerEntries.isEmpty()) {
            ledgerPort.saveAll(ledgerEntries);
        }

        log.info("Distribución de cuota {} del préstamo {}: capital={}, interés generado={}, comisión plataforma={}, rendimiento neto={} entre {} inversionista(s).",
                paidInstallment.getInstallmentNumber(), loan.getId(), paidInstallment.getPrincipalAmount(),
                margin.getGeneratedInterest(), margin.getPlatformCommission(), margin.getInvestorYield(), total);
    }
}
