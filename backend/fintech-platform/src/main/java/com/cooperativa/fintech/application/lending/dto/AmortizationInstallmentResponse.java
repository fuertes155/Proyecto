package com.cooperativa.fintech.application.lending.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AmortizationInstallmentResponse(
        int installmentNumber,
        BigDecimal paymentAmount,
        BigDecimal principalAmount,
        BigDecimal interestAmount,
        BigDecimal remainingBalance,
        LocalDate dueDate
) {
}
