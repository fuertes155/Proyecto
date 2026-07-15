package com.cooperativa.met.domain.investment.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class LedgerEntry {
    private final UUID id;
    private final UUID accountId;
    private final UUID transactionReference;
    private final LedgerEntryType entryType;
    private final BigDecimal amount;
    private final String concept;
    private final Instant createdAt;
}
