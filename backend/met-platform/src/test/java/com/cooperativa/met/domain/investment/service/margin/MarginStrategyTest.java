package com.cooperativa.met.domain.investment.service.margin;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarginStrategyTest {

    // Ejemplo de negocio: capital 100, interés generado 9.5 (9.5%), margen 2%.
    private static final BigDecimal CAPITAL = new BigDecimal("100");
    private static final BigDecimal GENERATED_INTEREST = new BigDecimal("9.5");
    private static final BigDecimal MARGIN_RATE = new BigDecimal("0.02");

    @Test
    void interestCommission_modelB_matchesBusinessExample() {
        MarginStrategy strategy = MarginStrategy.of(MarginModel.INTEREST_COMMISSION);

        MarginCalculationResult result = strategy.calculate(CAPITAL, GENERATED_INTEREST, MARGIN_RATE);

        assertEquals(new BigDecimal("0.19"), result.getPlatformCommission());
        assertEquals(new BigDecimal("9.31"), result.getInvestorYield());
        assertEquals(MarginModel.INTEREST_COMMISSION, result.getModel());
    }

    @Test
    void capitalCommission_modelC_matchesBusinessExample() {
        MarginStrategy strategy = MarginStrategy.of(MarginModel.CAPITAL_COMMISSION);

        MarginCalculationResult result = strategy.calculate(CAPITAL, GENERATED_INTEREST, MARGIN_RATE);

        assertEquals(new BigDecimal("2.00"), result.getPlatformCommission());
        assertEquals(new BigDecimal("7.50"), result.getInvestorYield());
    }

    @Test
    void directRateSubtraction_modelA_matchesBusinessExample() {
        MarginStrategy strategy = MarginStrategy.of(MarginModel.DIRECT_RATE_SUBTRACTION);

        MarginCalculationResult result = strategy.calculate(CAPITAL, GENERATED_INTEREST, MARGIN_RATE);

        assertEquals(new BigDecimal("2.00"), result.getPlatformCommission());
        assertEquals(new BigDecimal("7.50"), result.getInvestorYield());
    }

    @Test
    void modelA_and_modelC_areMathematicallyEquivalent() {
        MarginCalculationResult a = MarginStrategy.of(MarginModel.DIRECT_RATE_SUBTRACTION)
                .calculate(CAPITAL, GENERATED_INTEREST, MARGIN_RATE);
        MarginCalculationResult c = MarginStrategy.of(MarginModel.CAPITAL_COMMISSION)
                .calculate(CAPITAL, GENERATED_INTEREST, MARGIN_RATE);

        assertEquals(0, a.getInvestorYield().compareTo(c.getInvestorYield()));
        assertEquals(0, a.getPlatformCommission().compareTo(c.getPlatformCommission()));
    }

    @Test
    void interestCommission_scalesWithGeneratedInterest_notWithCapital() {
        // Con interés generado más bajo (deudor pagó menos), la comisión B debe bajar proporcionalmente,
        // a diferencia de C que solo depende del capital.
        MarginStrategy strategy = MarginStrategy.of(MarginModel.INTEREST_COMMISSION);

        MarginCalculationResult lowInterest = strategy.calculate(CAPITAL, new BigDecimal("4.0"), MARGIN_RATE);

        assertEquals(new BigDecimal("0.08"), lowInterest.getPlatformCommission());
    }

    @Test
    void rateBasis_nominalAnnual_dividesBy12() {
        BigDecimal monthly = RateBasis.NOMINAL_ANNUAL.toMonthlyRate(new BigDecimal("0.24"));
        assertEquals(0, monthly.compareTo(new BigDecimal("0.02")));
    }

    @Test
    void rateBasis_monthly_isPassThrough() {
        BigDecimal monthly = RateBasis.MONTHLY.toMonthlyRate(new BigDecimal("0.02"));
        assertEquals(0, monthly.compareTo(new BigDecimal("0.02")));
    }

    @Test
    void rateBasis_effectiveAnnual_usesTwelfthRoot_notSimpleDivision() {
        // Una EA del 12.68% ronda a ~1.00% mensual efectivo, distinto de 12.68/12=1.0567% nominal.
        BigDecimal monthly = RateBasis.EFFECTIVE_ANNUAL.toMonthlyRate(new BigDecimal("0.1268"));
        assertEquals(0, monthly.setScale(4, java.math.RoundingMode.HALF_UP)
                .compareTo(new BigDecimal("0.0100")));
    }
}
