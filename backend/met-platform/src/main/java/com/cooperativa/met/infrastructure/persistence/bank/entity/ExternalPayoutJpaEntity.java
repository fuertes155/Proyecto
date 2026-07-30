package com.cooperativa.met.infrastructure.persistence.bank.entity;

import com.cooperativa.met.domain.bank.model.ExternalPayout;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "external_payouts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalPayoutJpaEntity {

    @Id
    private UUID id;

    @Column(name = "core_transaction_id", nullable = false, unique = true)
    private UUID coreTransactionId;

    @Column(name = "external_bank_account_id", nullable = false)
    private UUID externalBankAccountId;

    @Column(name = "rail_reference")
    private String railReference;

    @Column(name = "failure_code")
    private String failureCode;

    @Column(name = "failure_message")
    private String failureMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "settled_at")
    private Instant settledAt;

    public ExternalPayout toDomain() {
        return ExternalPayout.builder()
                .id(id)
                .coreTransactionId(coreTransactionId)
                .externalBankAccountId(externalBankAccountId)
                .railReference(railReference)
                .failureCode(failureCode)
                .failureMessage(failureMessage)
                .createdAt(createdAt)
                .settledAt(settledAt)
                .build();
    }

    public static ExternalPayoutJpaEntity fromDomain(ExternalPayout domain) {
        return ExternalPayoutJpaEntity.builder()
                .id(domain.getId())
                .coreTransactionId(domain.getCoreTransactionId())
                .externalBankAccountId(domain.getExternalBankAccountId())
                .railReference(domain.getRailReference())
                .failureCode(domain.getFailureCode())
                .failureMessage(domain.getFailureMessage())
                .createdAt(domain.getCreatedAt())
                .settledAt(domain.getSettledAt())
                .build();
    }
}
