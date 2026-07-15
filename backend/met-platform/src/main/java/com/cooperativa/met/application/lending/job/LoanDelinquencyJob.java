package com.cooperativa.met.application.lending.job;

import com.cooperativa.met.infrastructure.persistence.lending.entity.PersonalLoanAmortizationJpaEntity;
import com.cooperativa.met.infrastructure.persistence.lending.repository.PersonalLoanAmortizationJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoanDelinquencyJob {

    private final PersonalLoanAmortizationJpaRepository amortizationRepository;

    // Usury rate limit set by Superintendencia Financiera (e.g. 28.5% annual -> ~0.078% daily)
    private static final BigDecimal DAILY_USURY_LIMIT = new BigDecimal("0.00078"); 
    private static final BigDecimal LATE_FEE_PENALTY_RATE = new BigDecimal("0.0005"); // 0.05% daily penalty

    /**
     * Executes every day at 00:01 AM
     */
    @Scheduled(cron = "0 1 0 * * ?")
    @Transactional
    public void evaluateDelinquency() {
        log.info("Starting Daily Loan Delinquency Evaluation Job at 00:01 AM");

        LocalDate today = LocalDate.now();

        // 1. Find newly overdue installments (Due date was yesterday or before)
        List<PersonalLoanAmortizationJpaEntity> newlyOverdue = amortizationRepository.findOverdueInstallments(today);
        for (PersonalLoanAmortizationJpaEntity installment : newlyOverdue) {
            log.warn("Installment {} is overdue. Marking as LATE.", installment.getId());
            installment.setStatus("LATE");
            if (installment.getPenaltyInterestAmount() == null) {
                installment.setPenaltyInterestAmount(BigDecimal.ZERO);
            }
            
            // Webhook Mock: Trigger SMS Day 1
            triggerCollectionSmsWebhook(installment, 1);
        }
        amortizationRepository.saveAll(newlyOverdue);

        // 2. Calculate daily penalty interest for all late installments
        List<PersonalLoanAmortizationJpaEntity> lateInstallments = amortizationRepository.findAllLateInstallments();
        for (PersonalLoanAmortizationJpaEntity installment : lateInstallments) {
            
            // Calculate penalty taking into account Usury Limits
            BigDecimal penaltyRateToApply = LATE_FEE_PENALTY_RATE.min(DAILY_USURY_LIMIT);
            
            BigDecimal dailyPenalty = installment.getPrincipalAmount()
                    .multiply(penaltyRateToApply)
                    .setScale(2, RoundingMode.HALF_UP);
            
            installment.setPenaltyInterestAmount(
                    installment.getPenaltyInterestAmount().add(dailyPenalty)
            );

            log.info("Added {} COP penalty interest to late installment {}. Total penalty: {}", 
                     dailyPenalty, installment.getId(), installment.getPenaltyInterestAmount());

            // 3. Attempt Recurring Tokenized Debit (Silent Charge)
            attemptTokenizedDebit(installment);
        }
        amortizationRepository.saveAll(lateInstallments);

        log.info("Finished Daily Loan Delinquency Evaluation Job.");
    }

    private void triggerCollectionSmsWebhook(PersonalLoanAmortizationJpaEntity installment, int daysLate) {
        // MOCK: In production this calls Twilio or AWS SNS
        log.info("[TWILIO WEBHOOK MOCK] Sending SMS for installment {}. Days late: {}", installment.getId(), daysLate);
        if (daysLate == 1) {
            log.info("   -> Mensaje: 'Cooperativa MET: Tu cuota ha vencido. Evita reportes negativos pagando hoy.'");
        } else if (daysLate >= 5) {
            log.info("   -> Mensaje: 'Cooperativa MET: PRE-REPORTE. Tienes 5 días de mora. Serás reportado a Datacrédito.'");
        }
    }

    private void attemptTokenizedDebit(PersonalLoanAmortizationJpaEntity installment) {
        // MOCK: Calls Wompi/MercadoPago tokenization API
        log.info("[PAYMENT GATEWAY MOCK] Attempting silent recurring debit for installment {} using saved card token...", installment.getId());
        // If successful -> status = PAID, update ledgers, etc.
    }
}
