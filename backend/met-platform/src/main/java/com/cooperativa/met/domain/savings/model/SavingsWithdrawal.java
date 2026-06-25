package com.cooperativa.met.domain.savings.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class SavingsWithdrawal {
    private final UUID id;
    private final UUID accountId;
    private final UUID userId;
    private final BigDecimal amount;
    private final WithdrawalType type;
    private final Instant createdAt;
}
