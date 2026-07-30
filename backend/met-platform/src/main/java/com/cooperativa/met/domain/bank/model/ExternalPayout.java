package com.cooperativa.met.domain.bank.model;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class ExternalPayout {
    private final UUID id;
    private final UUID coreTransactionId;
    private final UUID externalBankAccountId;
    private final String railReference;
    private final String failureCode;
    private final String failureMessage;
    private final Instant createdAt;
    private final Instant settledAt;

    public ExternalPayout withRailReference(String railReference) {
        return this.toBuilder().railReference(railReference).build();
    }

    public ExternalPayout markSettled() {
        return this.toBuilder().settledAt(Instant.now()).build();
    }

    public ExternalPayout markFailed(String code, String message) {
        return this.toBuilder()
                .failureCode(code)
                .failureMessage(message)
                .settledAt(Instant.now())
                .build();
    }
}
