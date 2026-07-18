package com.cooperativa.met.infrastructure.persistence.account.entity;

import com.cooperativa.met.domain.account.model.AccountStatus;
import com.cooperativa.met.domain.account.model.CoreAccount;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "core_accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoreAccountJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "account_number", nullable = false, unique = true)
    private String accountNumber;

    @Column(name = "principal_balance", nullable = false)
    private BigDecimal principalBalance;

    @Column(name = "interest_balance", nullable = false)
    private BigDecimal interestBalance;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AccountStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "daily_transfer_limit")
    private BigDecimal dailyTransferLimit;

    @Column(name = "per_transaction_limit")
    private BigDecimal perTransactionLimit;

    public CoreAccount toDomain() {
        return CoreAccount.builder()
                .id(id)
                .userId(userId)
                .accountNumber(accountNumber)
                .principalBalance(principalBalance)
                .interestBalance(interestBalance)
                .status(status)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .version(version)
                .dailyTransferLimit(dailyTransferLimit)
                .perTransactionLimit(perTransactionLimit)
                .build();
    }

    public static CoreAccountJpaEntity fromDomain(CoreAccount domain) {
        return CoreAccountJpaEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .accountNumber(domain.getAccountNumber())
                .principalBalance(domain.getPrincipalBalance())
                .interestBalance(domain.getInterestBalance())
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .version(domain.getVersion())
                .dailyTransferLimit(domain.getDailyTransferLimit())
                .perTransactionLimit(domain.getPerTransactionLimit())
                .build();
    }
}
