package com.cooperativa.met.domain.investment.model;

public enum GuaranteeFundMovementType {
    /** Dinero que entra al fondo (ej: cuota de Fondo de Garantías al desembolsar un préstamo). */
    CONTRIBUTION,
    /** Dinero que sale del fondo para cubrir (parcialmente) una mora. */
    PAYOUT
}
