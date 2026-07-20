package com.cooperativa.met.application.lending.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SimulateLoanRequest(
        @NotNull @DecimalMin("500000") @DecimalMax("50000000") BigDecimal amount,
        @Min(6) @Max(60) Integer termMonths
) {
}
