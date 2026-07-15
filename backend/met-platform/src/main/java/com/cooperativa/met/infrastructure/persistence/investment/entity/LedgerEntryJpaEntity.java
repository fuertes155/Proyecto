package com.cooperativa.met.infrastructure.persistence.investment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerEntryJpaEntity {

    @Id
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "transaction_reference", nullable = false)
    private UUID transactionReference;

    @Column(name = "entry_type", nullable = false)
    private String entryType;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column
    private String concept;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
