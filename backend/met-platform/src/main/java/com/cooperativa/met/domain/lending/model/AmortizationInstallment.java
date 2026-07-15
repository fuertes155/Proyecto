package com.cooperativa.met.domain.lending.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
public class AmortizationInstallment {

    private final UUID id;
    private final UUID applicationId;
    private final int installmentNumber;
    private final BigDecimal paymentAmount;
    private final BigDecimal principalAmount;
    private final BigDecimal interestAmount;
    private final BigDecimal remainingBalance;
    private final LocalDate dueDate;
    private final BigDecimal penaltyInterestAmount;
    private final String status; // PENDING, PAID, LATE

    public AmortizationInstallment withPenaltyInterestAmount(BigDecimal newPenaltyInterestAmount) {
        return builder()
                .id(id)
                .applicationId(applicationId)
                .installmentNumber(installmentNumber)
                .paymentAmount(paymentAmount)
                .principalAmount(principalAmount)
                .interestAmount(interestAmount)
                .remainingBalance(remainingBalance)
                .dueDate(dueDate)
                .penaltyInterestAmount(newPenaltyInterestAmount)
                .status(this.status)
                .build();
    }

    public AmortizationInstallment withStatus(String newStatus) {
        return builder()
                .id(id)
                .applicationId(applicationId)
                .installmentNumber(installmentNumber)
                .paymentAmount(paymentAmount)
                .principalAmount(principalAmount)
                .interestAmount(interestAmount)
                .remainingBalance(remainingBalance)
                .dueDate(dueDate)
                .penaltyInterestAmount(penaltyInterestAmount)
                .status(newStatus)
                .build();
    }
}
