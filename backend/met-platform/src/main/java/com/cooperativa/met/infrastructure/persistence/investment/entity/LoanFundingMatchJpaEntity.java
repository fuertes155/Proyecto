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
@Table(name = "loan_funding_matches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanFundingMatchJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "micro_investment_id", nullable = false)
    private UUID microInvestmentId;

    @Column(name = "loan_application_id", nullable = false)
    private UUID loanApplicationId;

    @Column(name = "investor_user_id", nullable = false)
    private UUID investorUserId;

    @Column(name = "borrower_user_id", nullable = false)
    private UUID borrowerUserId;

    @Column(name = "matched_amount", nullable = false)
    private BigDecimal matchedAmount;

    @Column(name = "matched_at", nullable = false)
    private Instant matchedAt;
}
