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
@Table(name = "investment_fractions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvestmentFractionJpaEntity {

    @Id
    private UUID id;

    @Column(name = "investor_account_id", nullable = false)
    private UUID investorAccountId;

    @Column(name = "original_deposit_id", nullable = false)
    private UUID originalDepositId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "matched_at")
    private Instant matchedAt;
}
