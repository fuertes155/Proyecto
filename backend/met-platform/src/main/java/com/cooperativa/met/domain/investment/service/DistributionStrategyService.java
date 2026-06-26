package com.cooperativa.met.domain.investment.service;

import com.cooperativa.met.domain.investment.model.InvestmentInstrument;
import com.cooperativa.met.domain.investment.model.InvestmentStrategy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio de dominio que calcula cómo distribuir el dinero entre instrumentos.
 *
 * EQUAL      → partes iguales entre todos los instrumentos disponibles.
 * WEIGHTED   → instrumentos ordenados por tasa; mayor peso a los de mayor plazo/tasa.
 * RISK_BASED → mayor proporción a instrumentos de menor tasa (más conservadores).
 */
public class DistributionStrategyService {

    private DistributionStrategyService() {}

    /**
     * Calcula la distribución del monto entre los instrumentos disponibles.
     *
     * @param montoTotal   Dinero total a distribuir.
     * @param instrumentos Lista de instrumentos activos y elegibles.
     * @param estrategia   Estrategia de distribución.
     * @return Map ordenado: instrumento → monto asignado.
     * @throws IllegalArgumentException si no hay instrumentos válidos o el monto es insuficiente.
     */
    public static Map<InvestmentInstrument, BigDecimal> distribuir(
            BigDecimal montoTotal,
            List<InvestmentInstrument> instrumentos,
            InvestmentStrategy estrategia) {

        List<InvestmentInstrument> elegibles = instrumentos.stream()
                .filter(i -> i.isActivo() && montoTotal.compareTo(i.getMontoMinimo()) >= 0)
                .toList();

        if (elegibles.isEmpty()) {
            throw new IllegalArgumentException(
                    "No hay instrumentos disponibles para el monto indicado (" + montoTotal + ")");
        }

        return switch (estrategia) {
            case EQUAL      -> distribuirIgualitario(montoTotal, elegibles);
            case WEIGHTED   -> distribuirPonderado(montoTotal, elegibles);
            case RISK_BASED -> distribuirPorRiesgo(montoTotal, elegibles);
        };
    }

    // ── EQUAL: partes iguales ─────────────────────────────────────────────────

    private static Map<InvestmentInstrument, BigDecimal> distribuirIgualitario(
            BigDecimal montoTotal, List<InvestmentInstrument> elegibles) {

        int n = elegibles.size();
        BigDecimal porcion = montoTotal.divide(BigDecimal.valueOf(n), 2, RoundingMode.FLOOR);
        BigDecimal residuo = montoTotal.subtract(porcion.multiply(BigDecimal.valueOf(n)));

        Map<InvestmentInstrument, BigDecimal> resultado = new LinkedHashMap<>();
        for (int i = 0; i < elegibles.size(); i++) {
            BigDecimal monto = (i == 0) ? porcion.add(residuo) : porcion;
            resultado.put(elegibles.get(i), monto);
        }
        return resultado;
    }

    // ── WEIGHTED: mayor peso a mayor tasa/plazo ───────────────────────────────

    private static Map<InvestmentInstrument, BigDecimal> distribuirPonderado(
            BigDecimal montoTotal, List<InvestmentInstrument> elegibles) {

        // Ordena de mayor a menor tasa; los mejores rendimientos reciben más dinero
        List<InvestmentInstrument> ordenados = elegibles.stream()
                .sorted(Comparator.comparing(InvestmentInstrument::getTasaAnual).reversed())
                .toList();

        // Suma total de tasas → peso proporcional
        BigDecimal sumaTasas = ordenados.stream()
                .map(InvestmentInstrument::getTasaAnual)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<InvestmentInstrument, BigDecimal> resultado = new LinkedHashMap<>();
        BigDecimal asignado = BigDecimal.ZERO;

        for (int i = 0; i < ordenados.size(); i++) {
            InvestmentInstrument inst = ordenados.get(i);
            BigDecimal monto;
            if (i == ordenados.size() - 1) {
                // El último recibe el residuo para evitar pérdida por redondeo
                monto = montoTotal.subtract(asignado);
            } else {
                monto = montoTotal
                        .multiply(inst.getTasaAnual())
                        .divide(sumaTasas, 2, RoundingMode.FLOOR);
                asignado = asignado.add(monto);
            }
            resultado.put(inst, monto);
        }
        return resultado;
    }

    // ── RISK_BASED: mayor proporción a instrumentos conservadores ─────────────

    private static Map<InvestmentInstrument, BigDecimal> distribuirPorRiesgo(
            BigDecimal montoTotal, List<InvestmentInstrument> elegibles) {

        // Riesgo inverso: los de menor tasa (más corto plazo, más líquidos) reciben más dinero
        List<InvestmentInstrument> ordenados = elegibles.stream()
                .sorted(Comparator.comparing(InvestmentInstrument::getTasaAnual))  // menor tasa = menor riesgo
                .toList();

        // Pesos inversos: 1/tasa normalizado
        BigDecimal sumaPesosInversos = ordenados.stream()
                .map(i -> BigDecimal.ONE.divide(i.getTasaAnual(), 10, RoundingMode.HALF_UP))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<InvestmentInstrument, BigDecimal> resultado = new LinkedHashMap<>();
        BigDecimal asignado = BigDecimal.ZERO;

        for (int i = 0; i < ordenados.size(); i++) {
            InvestmentInstrument inst = ordenados.get(i);
            BigDecimal monto;
            if (i == ordenados.size() - 1) {
                monto = montoTotal.subtract(asignado);
            } else {
                BigDecimal pesoInverso = BigDecimal.ONE.divide(inst.getTasaAnual(), 10, RoundingMode.HALF_UP);
                monto = montoTotal
                        .multiply(pesoInverso)
                        .divide(sumaPesosInversos, 2, RoundingMode.FLOOR);
                asignado = asignado.add(monto);
            }
            resultado.put(inst, monto);
        }
        return resultado;
    }
}
