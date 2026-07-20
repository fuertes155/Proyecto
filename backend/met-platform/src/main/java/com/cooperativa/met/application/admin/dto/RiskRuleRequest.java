package com.cooperativa.met.application.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record RiskRuleRequest(
        @NotBlank(message = "El nombre es requerido")
        String nombre,

        String descripcion,

        /**
         * JSON con la condición de la regla.
         * Ej: {"field":"amount","operator":"gt","value":10000000}
         */
        @NotBlank(message = "La condición es requerida")
        String condicion,

        /**
         * Acción: ALLOW, BLOCK, REVIEW
         */
        @NotBlank(message = "La acción es requerida")
        String accion,

        Boolean activo
) {}
