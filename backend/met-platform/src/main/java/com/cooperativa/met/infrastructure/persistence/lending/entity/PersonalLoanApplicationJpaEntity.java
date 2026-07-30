package com.cooperativa.met.infrastructure.persistence.lending.entity;

import com.cooperativa.met.domain.lending.model.LoanApplicationStatus;
import com.cooperativa.met.domain.lending.model.RiskTier;
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
@Table(name = "personal_loan_applications")
@Getter
@Setter
public class PersonalLoanApplicationJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "term_months", nullable = false)
    private int termMonths;

    @Column(name = "annual_interest_rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal annualInterestRate;

    @Column(name = "monthly_payment", nullable = false, precision = 18, scale = 2)
    private BigDecimal monthlyPayment;

    @Column(name = "total_interest", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalInterest;

    @Column(name = "total_payment", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalPayment;

    @Column(nullable = false)
    private String purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoanApplicationStatus status;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "credit_score")
    private Integer creditScore;

    @Column(name = "credit_bureau_ref")
    private String creditBureauRef;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_tier", length = 20)
    private RiskTier riskTier;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
