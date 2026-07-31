package com.cooperativa.met.domain.investment.service.margin;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Modelo A: r_i = r_d - m (resta directa sobre tasas).
 * Expresado en montos sobre el mismo capital base y periodo, esto equivale a
 * Comisión_plataforma = Capital × m — igual que CAPITAL_COMMISSION. Se mantiene
 * como estrategia separada porque el contrato/UI lo disclosea como spread de tasa,
 * no como comisión, y ambas lecturas deben poder evolucionar de forma independiente.
 */
class DirectRateSubtractionStrategy implements MarginStrategy {

    @Override
    public MarginModel getModel() {
        return MarginModel.DIRECT_RATE_SUBTRACTION;
    }

    @Override
    public MarginCalculationResult calculate(BigDecimal capitalBase, BigDecimal generatedInterest, BigDecimal marginRate) {
        BigDecimal commission = capitalBase.multiply(marginRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal yield = generatedInterest.subtract(commission);

        return MarginCalculationResult.builder()
                .model(getModel())
                .capitalBase(capitalBase)
                .generatedInterest(generatedInterest)
                .marginRate(marginRate)
                .platformCommission(commission)
                .investorYield(yield)
                .build();
    }
}
