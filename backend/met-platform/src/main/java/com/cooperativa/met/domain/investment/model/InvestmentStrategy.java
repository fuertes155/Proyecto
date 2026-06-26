package com.cooperativa.met.domain.investment.model;

/**
 * Estrategia de distribución del dinero entre instrumentos de inversión.
 *
 * EQUAL      — Se divide el monto en partes iguales entre todos los instrumentos disponibles.
 * WEIGHTED   — Cada instrumento tiene un peso porcentual configurado por el admin.
 * RISK_BASED — Se prioriza instrumentos con menor tasa (menor riesgo) para montos altos.
 */
public enum InvestmentStrategy {
    EQUAL,
    WEIGHTED,
    RISK_BASED
}
