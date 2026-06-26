package com.cooperativa.met.domain.investment.model;

/**
 * Estado de un portfolio o posición individual de inversión.
 */
public enum InvestmentStatus {
    /** Inversión vigente, aún no ha vencido. */
    ACTIVE,
    /** Inversión madurada: capital + rendimiento ya acreditados al usuario. */
    COMPLETED,
    /** Cancelada antes del vencimiento: solo se devuelve el capital. */
    CANCELLED
}
