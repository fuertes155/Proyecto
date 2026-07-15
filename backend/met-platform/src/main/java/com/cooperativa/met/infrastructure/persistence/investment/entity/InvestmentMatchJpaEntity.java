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

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "investment_matches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvestmentMatchJpaEntity {

    @Id
    private UUID id;

    @Column(name = "fraction_id", nullable = false)
    private UUID fractionId;

    @Column(name = "borrower_loan_id", nullable = false)
    private UUID borrowerLoanId;

    @Column(name = "matched_at", nullable = false)
    private Instant matchedAt;
}
