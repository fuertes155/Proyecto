package com.cooperativa.met.domain.investment.service;

import com.cooperativa.met.domain.investment.model.InvestmentInstrument;
import com.cooperativa.met.domain.investment.model.InvestmentStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DistributionStrategyServiceTest {

    private final InvestmentInstrument cdt90 = instrument("CDT 90 días", "0.08", "50000", true);
    private final InvestmentInstrument cdt180 = instrument("CDT 180 días", "0.10", "50000", true);
    private final InvestmentInstrument cdt360 = instrument("CDT 360 días", "0.12", "50000", true);

    @ParameterizedTest
    @EnumSource(InvestmentStrategy.class)
    void shouldNeverLoseOrInventAPeso(InvestmentStrategy strategy) {
        BigDecimal total = new BigDecimal("1000000.01");

        Map<InvestmentInstrument, BigDecimal> split = DistributionStrategyService.distribuir(total, List.of(cdt90, cdt180, cdt360), strategy);

        assertEquals(0, total.compareTo(split.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add)));
        assertEquals(3, split.size());
    }

    @Test
    void shouldSplitEquallyGivingTheRoundingRemainderToTheFirst() {
        Map<InvestmentInstrument, BigDecimal> split = DistributionStrategyService.distribuir(
                new BigDecimal("100000.00"), List.of(cdt90, cdt180, cdt360), InvestmentStrategy.EQUAL);

        assertEquals(new BigDecimal("33333.34"), split.get(cdt90));
        assertEquals(new BigDecimal("33333.33"), split.get(cdt180));
        assertEquals(new BigDecimal("33333.33"), split.get(cdt360));
    }

    @Test
    void shouldPutMoreMoneyInHigherRatesWhenWeighted() {
        Map<InvestmentInstrument, BigDecimal> split = DistributionStrategyService.distribuir(
                new BigDecimal("300000.00"), List.of(cdt90, cdt180, cdt360), InvestmentStrategy.WEIGHTED);

        assertEquals(cdt360, split.keySet().iterator().next(), "el de mayor tasa va primero");
        assertEquals(new BigDecimal("120000.00"), split.get(cdt360));
        assertEquals(new BigDecimal("100000.00"), split.get(cdt180));
        assertEquals(new BigDecimal("80000.00"), split.get(cdt90));
    }

    @Test
    void shouldPutMoreMoneyInConservativeInstrumentsWhenRiskBased() {
        Map<InvestmentInstrument, BigDecimal> split = DistributionStrategyService.distribuir(
                new BigDecimal("300000.00"), List.of(cdt360, cdt180, cdt90), InvestmentStrategy.RISK_BASED);

        assertTrue(split.get(cdt90).compareTo(split.get(cdt180)) > 0);
        assertTrue(split.get(cdt180).compareTo(split.get(cdt360)) > 0);
    }

    @Test
    void shouldSkipInactiveInstrumentsAndThoseAboveTheAmount() {
        InvestmentInstrument inactive = instrument("Pausado", "0.15", "1000", false);
        InvestmentInstrument tooBig = instrument("Premium", "0.20", "5000000", true);

        Map<InvestmentInstrument, BigDecimal> split = DistributionStrategyService.distribuir(
                new BigDecimal("100000.00"), List.of(inactive, tooBig, cdt90), InvestmentStrategy.EQUAL);

        assertEquals(Map.of(cdt90, new BigDecimal("100000.00")), split);
        assertFalse(split.containsKey(inactive));
    }

    @Test
    void shouldFailWhenNoInstrumentFits() {
        assertThrows(IllegalArgumentException.class, () -> DistributionStrategyService.distribuir(
                new BigDecimal("10000.00"), List.of(cdt90, cdt180), InvestmentStrategy.WEIGHTED));
    }

    private static InvestmentInstrument instrument(String nombre, String tasa, String minimo, boolean activo) {
        return InvestmentInstrument.builder()
                .id(UUID.randomUUID())
                .nombre(nombre)
                .tasaAnual(new BigDecimal(tasa))
                .plazoDias(90)
                .montoMinimo(new BigDecimal(minimo))
                .activo(activo)
                .build();
    }
}
