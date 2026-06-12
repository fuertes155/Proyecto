package com.cooperativa.fintech.application.savings.dto;

import com.cooperativa.fintech.domain.savings.model.ContributionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ContributionResponse(
        UUID id,
        UUID accountId,
        BigDecimal amount,
        LocalDate scheduledDate,
        Instant executedAt,
        ContributionStatus status,
        String failureReason
) {
}
