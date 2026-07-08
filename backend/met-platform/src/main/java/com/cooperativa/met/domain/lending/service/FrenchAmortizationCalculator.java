package com.cooperativa.met.domain.lending.service;

import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.lending.model.AmortizationInstallment;
import com.cooperativa.met.domain.lending.model.LoanSimulationResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class FrenchAmortizationCalculator {

    private static final BigDecimal MIN_AMOUNT = new BigDecimal("500000");
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("50000000");
    private static final int MIN_TERM = 6;
    private static final int MAX_TERM = 60;
    private static final BigDecimal DEFAULT_ANNUAL_RATE = new BigDecimal("0.2400");

    private FrenchAmortizationCalculator() {
    }

    public static LoanSimulationResult simulate(
            BigDecimal amount,
            int termMonths,
            BigDecimal annualInterestRate,
            LocalDate startDate) {

        validate(amount, termMonths);

        BigDecimal annualRate = annualInterestRate != null ? annualInterestRate : DEFAULT_ANNUAL_RATE;
        BigDecimal monthlyRate = effectiveAnnualToMonthly(annualRate);
        BigDecimal monthlyPayment = calculateMonthlyPayment(amount, monthlyRate, termMonths);

        List<AmortizationInstallment> schedule = buildSchedule(
                amount, monthlyRate, monthlyPayment, termMonths, startDate);

        BigDecimal totalPayment = schedule.stream()
                .map(AmortizationInstallment::getPaymentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalInterest = totalPayment.subtract(amount);

        return LoanSimulationResult.builder()
                .amount(amount)
                .termMonths(termMonths)
                .annualInterestRate(annualRate)
                .monthlyInterestRate(monthlyRate)
                .monthlyPayment(monthlyPayment)
                .totalInterest(totalInterest)
                .totalPayment(totalPayment)
                .schedule(schedule)
                .build();
    }

    public static BigDecimal effectiveAnnualToMonthly(BigDecimal annualRate) {
        // Usamos Tasa Nominal (TNA -> Mensual) para evitar pérdida de precisión con raíces
        return annualRate.divide(BigDecimal.valueOf(12), 8, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculateMonthlyPayment(BigDecimal principal, BigDecimal monthlyRate, int termMonths) {
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(termMonths), 2, RoundingMode.HALF_UP);
        }
        
        // factor = (1 + r)^n
        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
        BigDecimal factor = onePlusR.pow(termMonths);
        
        // numerator = principal * r * factor
        BigDecimal numerator = principal.multiply(monthlyRate).multiply(factor);
        
        // denominator = factor - 1
        BigDecimal denominator = factor.subtract(BigDecimal.ONE);
        
        // payment = numerator / denominator
        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private static List<AmortizationInstallment> buildSchedule(
            BigDecimal principal,
            BigDecimal monthlyRate,
            BigDecimal monthlyPayment,
            int termMonths,
            LocalDate startDate) {

        List<AmortizationInstallment> schedule = new ArrayList<>();
        BigDecimal balance = principal;

        for (int i = 1; i <= termMonths; i++) {
            BigDecimal interest = balance.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal principalPart = monthlyPayment.subtract(interest);
            if (i == termMonths) {
                principalPart = balance;
                monthlyPayment = principalPart.add(interest);
            }
            balance = balance.subtract(principalPart).max(BigDecimal.ZERO);

            schedule.add(AmortizationInstallment.builder()
                    .installmentNumber(i)
                    .paymentAmount(monthlyPayment)
                    .principalAmount(principalPart)
                    .interestAmount(interest)
                    .remainingBalance(balance)
                    .dueDate(startDate.plusMonths(i))
                    .build());
        }
        return schedule;
    }

    private static void validate(BigDecimal amount, int termMonths) {
        if (amount.compareTo(MIN_AMOUNT) < 0 || amount.compareTo(MAX_AMOUNT) > 0) {
            throw new BusinessRuleException("INVALID_AMOUNT",
                    "Monto debe estar entre $500.000 y $50.000.000");
        }
        if (termMonths < MIN_TERM || termMonths > MAX_TERM) {
            throw new BusinessRuleException("INVALID_TERM", "Plazo debe estar entre 6 y 60 meses");
        }
    }
}
