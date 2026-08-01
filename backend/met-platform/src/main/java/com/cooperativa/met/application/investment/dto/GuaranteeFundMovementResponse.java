package com.cooperativa.met.application.investment.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record GuaranteeFundMovementResponse(
        UUID id,
        String type,
        BigDecimal amount,
        UUID transactionReference,
        String concept,
        Instant createdAt
) {
}
