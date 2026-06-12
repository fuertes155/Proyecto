package com.cooperativa.fintech.application.solidarity.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ContributeToPoolRequest(
        @NotNull @DecimalMin(value = "1000.00", message = "Aporte mínimo: $1.000") BigDecimal amount
) {
}
