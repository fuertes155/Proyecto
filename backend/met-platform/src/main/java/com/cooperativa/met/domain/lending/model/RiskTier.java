package com.cooperativa.met.domain.lending.model;

import lombok.Getter;

import java.math.BigDecimal;

/**
 * Bandas de riesgo derivadas del score de DataCrédito. Tabla de política de crédito
 * (acordada con el equipo de Riesgo) que centraliza, para cada banda, el multiplicador
 * de saldo de ahorro que determina el monto máximo, el tope absoluto en COP, el plazo
 * máximo en meses y la tasa anual aplicable.
 *
 * Es la única fuente de verdad de estas reglas: ningún otro módulo debe hardcodear
 * montos, plazos o tasas por perfil de riesgo.
 */
@Getter
public enum RiskTier {

    RECHAZADO(0, null, null, 0, null),
    RIESGO_ALTO(600, new BigDecimal("1"), new BigDecimal("5000000"), 12, new BigDecimal("0.26")),
    RIESGO_MEDIO(700, new BigDecimal("2"), new BigDecimal("15000000"), 24, new BigDecimal("0.22")),
    RIESGO_BAJO(800, new BigDecimal("3"), new BigDecimal("30000000"), 36, new BigDecimal("0.19")),
    PRIME(900, new BigDecimal("5"), new BigDecimal("60000000"), 48, new BigDecimal("0.15"));

    private final int minScore;
    private final BigDecimal savingsMultiplier;
    private final BigDecimal absoluteCapCop;
    private final int maxTermMonths;
    private final BigDecimal annualInterestRate;

    RiskTier(int minScore, BigDecimal savingsMultiplier, BigDecimal absoluteCapCop,
            int maxTermMonths, BigDecimal annualInterestRate) {
        this.minScore = minScore;
        this.savingsMultiplier = savingsMultiplier;
        this.absoluteCapCop = absoluteCapCop;
        this.maxTermMonths = maxTermMonths;
        this.annualInterestRate = annualInterestRate;
    }

    /**
     * Determina la banda aplicable para un score dado, tomando la banda de mayor
     * minScore que el score todavía satisface (las bandas se declaran en orden ascendente).
     */
    public static RiskTier fromScore(int score) {
        RiskTier result = RECHAZADO;
        for (RiskTier tier : values()) {
            if (score >= tier.minScore) {
                result = tier;
            }
        }
        return result;
    }

    public boolean isApproved() {
        return this != RECHAZADO;
    }
}
