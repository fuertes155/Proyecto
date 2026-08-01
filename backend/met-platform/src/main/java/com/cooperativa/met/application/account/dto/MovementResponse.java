package com.cooperativa.met.application.account.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MovementResponse(
        UUID id,
        String type,
        String typeLabel,
        String concept,
        BigDecimal amount,
        boolean isCredit,
        String status,
        Instant createdAt
) {
}
