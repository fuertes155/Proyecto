package com.cooperativa.met.application.investment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Request para crear un portfolio de micro-inversiones.
 * El sistema distribuirá el montoTotal entre los instrumentos disponibles
 * según la estrategia elegida.
 */
public record CreatePortfolioRequest(

        @NotNull(message = "El monto total es requerido")
        @DecimalMin(value = "5000", message = "El monto mínimo de inversión es $5.000")
        BigDecimal montoTotal,

        /**
         * Estrategia: EQUAL, WEIGHTED o RISK_BASED.
         * Por defecto EQUAL si no se envía.
         */
        String estrategia

) {}
