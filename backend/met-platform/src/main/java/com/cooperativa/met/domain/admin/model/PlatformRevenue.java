package com.cooperativa.met.domain.admin.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class PlatformRevenue {
    private final UUID id;
    private final UUID userId;
    private final BigDecimal amount;
    private final String description;
    private final String source; // e.g., "SCHEDULED_SAVINGS_DEPOSIT"
    private final Instant createdAt;
}
