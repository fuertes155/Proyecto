package com.cooperativa.met.domain.investment.service.margin;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Modelo B (recomendado): Comisión_plataforma = Interés_generado × m.
 * Rendimiento_inversionista = Interés_generado - Comisión_plataforma.
 */
class InterestCommissionStrategy implements MarginStrategy {

    @Override
    public MarginModel getModel() {
        return MarginModel.INTEREST_COMMISSION;
    }

    @Override
    public MarginCalculationResult calculate(BigDecimal capitalBase, BigDecimal generatedInterest, BigDecimal marginRate) {
        BigDecimal commission = generatedInterest.multiply(marginRate).setScale(2, RoundingMode.HALF_UP);
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
