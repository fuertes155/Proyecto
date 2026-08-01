package com.cooperativa.met.application.lending.dto;

import java.math.BigDecimal;

public record LoanEligibilityResponse(
        boolean approved,
        String tier,
        int score,
        BigDecimal maxAmount,
        Integer maxTermMonths,
        BigDecimal annualInterestRate,
        String reason
) {
}
