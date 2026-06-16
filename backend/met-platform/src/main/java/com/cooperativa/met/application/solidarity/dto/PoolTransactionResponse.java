package com.cooperativa.met.application.solidarity.dto;

import com.cooperativa.met.domain.solidarity.model.PoolTransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PoolTransactionResponse(
        UUID id,
        PoolTransactionType type,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String description,
        Instant createdAt
) {
}
