package com.cooperativa.met.application.solidarity.dto;

import com.cooperativa.met.domain.solidarity.model.InstallmentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record LoanInstallmentResponse(
        UUID id,
        UUID loanId,
        int installmentNumber,
        BigDecimal principalAmount,
        BigDecimal interestAmount,
        BigDecimal totalAmount,
        LocalDate dueDate,
        Instant paidAt,
        InstallmentStatus status
) {
}
