package com.cooperativa.met.application.admin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public record FeeScheduleRequest(
        @NotBlank(message = "El tipo de tarifa es requerido")
        String tipoTarifa,

        String descripcion,

        @NotNull(message = "El valor de la tarifa es requerido")
        @DecimalMin(value = "0.0", inclusive = false, message = "El valor debe ser mayor a 0")
        BigDecimal valor,

        Boolean esPorcentaje,

        @NotNull(message = "La fecha de inicio de vigencia es requerida")
        Instant vigentDesde
) {}
