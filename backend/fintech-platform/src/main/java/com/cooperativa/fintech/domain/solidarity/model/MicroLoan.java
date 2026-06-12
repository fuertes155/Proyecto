package com.cooperativa.fintech.domain.solidarity.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class MicroLoan {

    private final UUID id;
    private final UUID groupId;
    private final UUID borrowerId;
    private final BigDecimal amount;
    private final String purpose;
    private final int termMonths;
    private final BigDecimal interestRate;
    private final MicroLoanStatus status;
    private final Instant requestedAt;
    private final Instant reviewedAt;
    private final UUID reviewedBy;
    private final Instant disbursedAt;
    private final String rejectionReason;

    public MicroLoan approve(UUID reviewerId) {
        return toBuilder()
                .status(MicroLoanStatus.APPROVED)
                .reviewedAt(Instant.now())
                .reviewedBy(reviewerId)
                .build();
    }

    public MicroLoan reject(UUID reviewerId, String reason) {
        return toBuilder()
                .status(MicroLoanStatus.REJECTED)
                .reviewedAt(Instant.now())
                .reviewedBy(reviewerId)
                .rejectionReason(reason)
                .build();
    }

    public MicroLoan disburse() {
        return toBuilder()
                .status(MicroLoanStatus.DISBURSED)
                .disbursedAt(Instant.now())
                .build();
    }

    public MicroLoan markRepaid() {
        return toBuilder().status(MicroLoanStatus.REPAID).build();
    }
}
