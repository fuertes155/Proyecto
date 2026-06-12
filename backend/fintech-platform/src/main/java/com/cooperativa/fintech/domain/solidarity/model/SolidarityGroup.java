package com.cooperativa.fintech.domain.solidarity.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class SolidarityGroup {

    private final UUID id;
    private final String name;
    private final String description;
    private final UUID creatorId;
    private final String inviteCode;
    private final BigDecimal minContribution;
    private final BigDecimal maxLoanPercentage;
    private final BigDecimal interestRate;
    private final BigDecimal poolBalance;
    private final int maxMembers;
    private final GroupStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;

    public SolidarityGroup withPoolBalance(BigDecimal balance) {
        return toBuilder().poolBalance(balance).updatedAt(Instant.now()).build();
    }

    public SolidarityGroup withStatus(GroupStatus newStatus) {
        return toBuilder().status(newStatus).updatedAt(Instant.now()).build();
    }

    public BigDecimal maxLoanAmount() {
        return poolBalance.multiply(maxLoanPercentage)
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
    }
}
