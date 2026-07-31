package com.cooperativa.met.domain.investment.service.margin;

import java.math.BigDecimal;

/**
 * Estrategia intercambiable para calcular cómo se reparte el interés generado
 * por una cuota entre la comisión de la plataforma y el rendimiento del inversionista.
 * Ver {@link MarginModel} para el detalle de cada modelo.
 */
public interface MarginStrategy {

    MarginModel getModel();

    /**
     * @param capitalBase       capital sobre el que se generó el interés (relevante para CAPITAL_COMMISSION
     *                          y DIRECT_RATE_SUBTRACTION; INTEREST_COMMISSION no lo usa para el cálculo).
     * @param generatedInterest interés realmente generado en el periodo (ej: interestAmount de la cuota).
     * @param marginRate        tasa de margen de la plataforma (ej: 0.02 = 2%).
     */
    MarginCalculationResult calculate(BigDecimal capitalBase, BigDecimal generatedInterest, BigDecimal marginRate);

    static MarginStrategy of(MarginModel model) {
        return switch (model) {
            case DIRECT_RATE_SUBTRACTION -> new DirectRateSubtractionStrategy();
            case INTEREST_COMMISSION -> new InterestCommissionStrategy();
            case CAPITAL_COMMISSION -> new CapitalCommissionStrategy();
        };
    }
}
