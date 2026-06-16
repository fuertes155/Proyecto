package com.cooperativa.met.application.savings.dto;

import com.cooperativa.met.domain.savings.model.ContributionFrequency;
import com.cooperativa.met.domain.savings.model.ScheduledSavingsStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ScheduledSavingsResponse(
        UUID id,
        String name,
        BigDecimal targetAmount,
        BigDecimal contributionAmount,
        ContributionFrequency frequency,
        Integer debitDayOfWeek,
        Integer debitDayOfMonth,
        BigDecimal currentBalance,
        BigDecimal progressPercentage,
        ScheduledSavingsStatus status,
        LocalDate nextContributionDate,
        Instant createdAt
) {
}
