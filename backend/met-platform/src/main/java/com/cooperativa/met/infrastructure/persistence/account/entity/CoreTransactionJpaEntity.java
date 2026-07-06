package com.cooperativa.met.infrastructure.persistence.account.entity;

import com.cooperativa.met.domain.account.model.CoreTransaction;
import com.cooperativa.met.domain.account.model.TransactionStatus;
import com.cooperativa.met.domain.account.model.TransactionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "core_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoreTransactionJpaEntity {

    @Id
    private UUID id;

    @Column(name = "source_account_id")
    private UUID sourceAccountId;

    @Column(name = "destination_account_id")
    private UUID destinationAccountId;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "concept")
    private String concept;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TransactionStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public CoreTransaction toDomain() {
        return CoreTransaction.builder()
                .id(id)
                .sourceAccountId(sourceAccountId)
                .destinationAccountId(destinationAccountId)
                .amount(amount)
                .concept(concept)
                .type(type)
                .status(status)
                .createdAt(createdAt)
                .build();
    }

    public static CoreTransactionJpaEntity fromDomain(CoreTransaction domain) {
        return CoreTransactionJpaEntity.builder()
                .id(domain.getId())
                .sourceAccountId(domain.getSourceAccountId())
                .destinationAccountId(domain.getDestinationAccountId())
                .amount(domain.getAmount())
                .concept(domain.getConcept())
                .type(domain.getType())
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
