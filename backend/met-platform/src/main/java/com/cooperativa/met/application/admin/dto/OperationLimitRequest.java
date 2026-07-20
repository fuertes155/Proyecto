package com.cooperativa.met.application.admin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record OperationLimitRequest(
        @NotBlank(message = "El tipo de operación es requerido")
        String tipoOperacion,

        @Min(value = 1, message = "El monto diario debe ser mayor a 0")
        Long montoDiarioMax,

        @Min(value = 1, message = "El monto por transacción debe ser mayor a 0")
        Long montoPorTransaccionMax
) {}
