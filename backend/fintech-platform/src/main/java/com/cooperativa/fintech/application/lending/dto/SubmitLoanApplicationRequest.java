package com.cooperativa.fintech.application.lending.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record SubmitLoanApplicationRequest(
        @NotNull @DecimalMin("500000") @DecimalMax("50000000") BigDecimal amount,
        @Min(6) @Max(60) int termMonths,
        @DecimalMin("0.01") @DecimalMax("1.00") BigDecimal annualInterestRate,
        @NotBlank @Size(max = 255) String purpose
) {
}
