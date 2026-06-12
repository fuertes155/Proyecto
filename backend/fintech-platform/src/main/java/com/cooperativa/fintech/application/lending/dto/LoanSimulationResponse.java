package com.cooperativa.fintech.application.lending.dto;

import java.math.BigDecimal;
import java.util.List;

public record LoanSimulationResponse(
        BigDecimal amount,
        int termMonths,
        BigDecimal annualInterestRate,
        BigDecimal monthlyInterestRate,
        BigDecimal monthlyPayment,
        BigDecimal totalInterest,
        BigDecimal totalPayment,
        List<AmortizationInstallmentResponse> schedule
) {
}
