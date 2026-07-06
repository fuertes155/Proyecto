package com.cooperativa.met.domain.account.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class CoreAccount {
    private final UUID id;
    private final UUID userId;
    private final String accountNumber;
    private final BigDecimal balance;
    private final AccountStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;

    public CoreAccount credit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        return this.toBuilder()
                .balance(this.balance.add(amount))
                .updatedAt(Instant.now())
                .build();
    }

    public CoreAccount debit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        if (this.balance.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient funds");
        }
        return this.toBuilder()
                .balance(this.balance.subtract(amount))
                .updatedAt(Instant.now())
                .build();
    }
}
