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
    private final Long version;
    private final BigDecimal dailyTransferLimit;
    private final BigDecimal perTransactionLimit;

    private static final BigDecimal DEFAULT_DAILY_LIMIT = new BigDecimal("5000000.00");
    private static final BigDecimal DEFAULT_TX_LIMIT = new BigDecimal("2000000.00");

    @Builder
    public CoreAccount(UUID id, UUID userId, String accountNumber,
                       BigDecimal principalBalance, BigDecimal interestBalance, AccountStatus status,
                       Instant createdAt, Instant updatedAt, Long version,
                       BigDecimal dailyTransferLimit, BigDecimal perTransactionLimit) {
        this.id = id;
        this.userId = userId;
        this.accountNumber = accountNumber;
        this.principalBalance = principalBalance;
        this.interestBalance = interestBalance;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
        this.dailyTransferLimit = dailyTransferLimit != null ? dailyTransferLimit : DEFAULT_DAILY_LIMIT;
        this.perTransactionLimit = perTransactionLimit != null ? perTransactionLimit : DEFAULT_TX_LIMIT;
    }

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

    public CoreAccount debitPrincipal(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        if (this.principalBalance.compareTo(amount) < 0) {
            throw new IllegalStateException("Fondos insuficientes");
        }
        return this.toBuilder()
                .principalBalance(this.principalBalance.subtract(amount))
                .updatedAt(Instant.now())
                .build();
    }
}
