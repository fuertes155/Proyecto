package com.cooperativa.fintech.domain.solidarity.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class PoolTransaction {

    private final UUID id;
    private final UUID groupId;
    private final UUID memberId;
    private final UUID loanId;
    private final PoolTransactionType type;
    private final BigDecimal amount;
    private final BigDecimal balanceAfter;
    private final String description;
    private final Instant createdAt;
}
