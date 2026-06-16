package com.cooperativa.met.domain.savings.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class ScheduledSavingsAccount {

    private final UUID id;
    private final UUID userId;
    private final String name;
    private final BigDecimal targetAmount;
    private final BigDecimal contributionAmount;
    private final ContributionFrequency frequency;
    private final Integer debitDayOfWeek;
    private final Integer debitDayOfMonth;
    private final BigDecimal currentBalance;
    private final ScheduledSavingsStatus status;
    private final LocalDate nextContributionDate;
    private final Instant createdAt;
    private final Instant updatedAt;

    public ScheduledSavingsAccount withBalance(BigDecimal newBalance) {
        return toBuilder().currentBalance(newBalance).updatedAt(Instant.now()).build();
    }

    public ScheduledSavingsAccount withStatus(ScheduledSavingsStatus newStatus) {
        return toBuilder().status(newStatus).updatedAt(Instant.now()).build();
    }

    public ScheduledSavingsAccount withNextContributionDate(LocalDate date) {
        return toBuilder().nextContributionDate(date).updatedAt(Instant.now()).build();
    }

    public ScheduledSavingsAccount withContributionAmount(BigDecimal amount) {
        return toBuilder().contributionAmount(amount).updatedAt(Instant.now()).build();
    }

    public boolean isTargetReached() {
        return targetAmount != null && currentBalance.compareTo(targetAmount) >= 0;
    }
}
