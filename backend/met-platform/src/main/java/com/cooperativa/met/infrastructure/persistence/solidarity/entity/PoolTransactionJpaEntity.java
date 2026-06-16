package com.cooperativa.met.infrastructure.persistence.solidarity.entity;

import com.cooperativa.met.domain.solidarity.model.PoolTransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pool_transactions")
@Getter
@Setter
public class PoolTransactionJpaEntity {

    @Id
    private UUID id;

    @Column(name = "group_id", nullable = false)
    private UUID groupId;

    @Column(name = "member_id")
    private UUID memberId;

    @Column(name = "loan_id")
    private UUID loanId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PoolTransactionType type;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false, precision = 18, scale = 2)
    private BigDecimal balanceAfter;

    @Column(length = 255)
    private String description;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
