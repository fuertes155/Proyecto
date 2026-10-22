package com.cooperativa.met.application.admin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OperationLimitRequest(
        @NotBlank(message = "El tipo de operación es requerido")
        String tipoOperacion,

        @NotNull(message = "El monto diario es requerido")
        @Min(value = 1, message = "El monto diario debe ser mayor a 0")
        Long montoDiarioMax,

        @NotNull(message = "El monto por transacción es requerido")
        @Min(value = 1, message = "El monto por transacción debe ser mayor a 0")
        Long montoPorTransaccionMax
) {}
