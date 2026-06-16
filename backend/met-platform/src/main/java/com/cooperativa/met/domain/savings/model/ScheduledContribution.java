package com.cooperativa.met.domain.savings.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class ScheduledContribution {

    private final UUID id;
    private final UUID accountId;
    private final BigDecimal amount;
    private final LocalDate scheduledDate;
    private final Instant executedAt;
    private final ContributionStatus status;
    private final String failureReason;
    private final Instant createdAt;

    public ScheduledContribution markCompleted() {
        return toBuilder()
                .status(ContributionStatus.COMPLETED)
                .executedAt(Instant.now())
                .build();
    }

    public ScheduledContribution markFailed(String reason) {
        return toBuilder()
                .status(ContributionStatus.FAILED)
                .failureReason(reason)
                .executedAt(Instant.now())
                .build();
    }
}
