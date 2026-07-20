package com.cooperativa.met.application.lending.dto;

import com.cooperativa.met.domain.lending.model.LoanApplicationStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LoanApplicationResponse(
        UUID id,
        BigDecimal amount,
        Integer termMonths,
        BigDecimal annualInterestRate,
        BigDecimal monthlyPayment,
        BigDecimal totalInterest,
        BigDecimal totalPayment,
        String purpose,
        LoanApplicationStatus status,
        String rejectionReason,
        Instant submittedAt,
        List<AmortizationInstallmentResponse> schedule
) {
}
