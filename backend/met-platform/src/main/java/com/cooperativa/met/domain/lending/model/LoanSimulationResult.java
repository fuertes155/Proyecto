package com.cooperativa.met.domain.lending.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class LoanSimulationResult {

    private final BigDecimal amount;
    private final int termMonths;
    private final BigDecimal annualInterestRate;
    private final BigDecimal monthlyInterestRate;
    private final BigDecimal monthlyPayment;
    private final BigDecimal totalInterest;
    private final BigDecimal totalPayment;
    private final List<AmortizationInstallment> schedule;
}
