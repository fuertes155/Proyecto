package com.cooperativa.met.domain.lending.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class PersonalLoanApplication {

    private final UUID id;
    private final UUID userId;
    private final BigDecimal amount;
    private final int termMonths;
    private final BigDecimal annualInterestRate;
    private final BigDecimal monthlyPayment;
    private final BigDecimal totalInterest;
    private final BigDecimal totalPayment;
    private final String purpose;
    private final LoanApplicationStatus status;
    private final String rejectionReason;
    
    // Integración de Central de Riesgo
    private final Integer creditScore;
    private final String creditBureauRef;
    private final RiskTier riskTier;
    
    private final Instant submittedAt;
    private final Instant reviewedAt;
    private final Instant createdAt;
    private final Instant updatedAt;

    public PersonalLoanApplication withStatus(LoanApplicationStatus newStatus) {
        return toBuilder().status(newStatus).updatedAt(Instant.now()).build();
    }

    public PersonalLoanApplication reject(String reason) {
        return toBuilder()
                .status(LoanApplicationStatus.REJECTED)
                .rejectionReason(reason)
                .reviewedAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
