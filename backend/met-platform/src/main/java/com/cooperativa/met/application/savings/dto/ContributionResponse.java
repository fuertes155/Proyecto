package com.cooperativa.met.application.savings.dto;

import com.cooperativa.met.domain.savings.model.ContributionStatus;

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
