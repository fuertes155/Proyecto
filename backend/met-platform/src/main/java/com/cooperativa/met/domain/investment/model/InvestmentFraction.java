package com.cooperativa.met.domain.investment.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class InvestmentFraction {
    private final UUID id;
    private final UUID investorAccountId;
    private final UUID originalDepositId;
    private final BigDecimal amount;
    private final InvestmentFractionStatus status;
    private final Instant createdAt;
    private final Instant matchedAt;

    public InvestmentFraction withStatus(InvestmentFractionStatus newStatus) {
        return toBuilder()
                .status(newStatus)
                .matchedAt(newStatus == InvestmentFractionStatus.MATCHED ? Instant.now() : this.matchedAt)
                .build();
    }
}
