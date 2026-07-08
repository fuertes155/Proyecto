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
    private final BigDecimal principalBalance;
    private final BigDecimal interestBalance;
    private final AccountStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;

    public CoreAccount creditPrincipal(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        return this.toBuilder()
                .principalBalance(this.principalBalance.add(amount))
                .updatedAt(Instant.now())
                .build();
    }

    public CoreAccount creditInterest(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        return this.toBuilder()
                .interestBalance(this.interestBalance.add(amount))
                .updatedAt(Instant.now())
                .build();
    }

    public CoreAccount debitInterest(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        if (this.interestBalance.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient interest funds");
        }
        return this.toBuilder()
                .interestBalance(this.interestBalance.subtract(amount))
                .updatedAt(Instant.now())
                .build();
    }
}
