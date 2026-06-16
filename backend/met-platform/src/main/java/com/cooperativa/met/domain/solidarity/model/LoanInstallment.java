package com.cooperativa.met.domain.solidarity.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class LoanInstallment {

    private final UUID id;
    private final UUID loanId;
    private final int installmentNumber;
    private final BigDecimal principalAmount;
    private final BigDecimal interestAmount;
    private final BigDecimal totalAmount;
    private final LocalDate dueDate;
    private final Instant paidAt;
    private final InstallmentStatus status;

    public LoanInstallment markPaid() {
        return toBuilder()
                .status(InstallmentStatus.PAID)
                .paidAt(Instant.now())
                .build();
    }
}
