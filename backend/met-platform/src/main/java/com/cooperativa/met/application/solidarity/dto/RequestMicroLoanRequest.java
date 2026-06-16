package com.cooperativa.met.application.solidarity.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record RequestMicroLoanRequest(
        @NotNull @DecimalMin(value = "10000.00", message = "Monto mínimo: $10.000") BigDecimal amount,
        @NotBlank @Size(max = 255) String purpose,
        @Min(1) @Max(24) int termMonths
) {
}
