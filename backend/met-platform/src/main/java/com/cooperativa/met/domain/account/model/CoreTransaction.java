package com.cooperativa.met.domain.account.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class CoreTransaction {
    private final UUID id;
    private final UUID sourceAccountId;
    private final UUID destinationAccountId;
    private final BigDecimal amount;
    private final String concept;
    private final TransactionType type;
    private final TransactionStatus status;
    private final Instant createdAt;
}
