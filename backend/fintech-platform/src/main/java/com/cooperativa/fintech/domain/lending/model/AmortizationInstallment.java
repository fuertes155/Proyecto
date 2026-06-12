package com.cooperativa.fintech.domain.lending.model;

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
}
