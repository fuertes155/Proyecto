package com.cooperativa.met.application.solidarity.dto;

import com.cooperativa.met.domain.solidarity.model.MicroLoanStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MicroLoanResponse(
        UUID id,
        UUID groupId,
        UUID borrowerId,
        BigDecimal amount,
        String purpose,
        int termMonths,
        BigDecimal interestRate,
        MicroLoanStatus status,
        Instant requestedAt,
        Instant reviewedAt,
        Instant disbursedAt,
        String rejectionReason
) {
}
