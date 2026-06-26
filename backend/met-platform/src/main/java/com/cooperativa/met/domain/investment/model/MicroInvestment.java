package com.cooperativa.met.domain.investment.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Posición individual de micro-inversión.
 * Cada posición corresponde a un instrumento dentro de un portfolio.
 * Al vencer, se calcula el rendimiento y se acredita al usuario.
 */
@Getter
@Builder(toBuilder = true)
public class MicroInvestment {

    private final UUID id;
    private final UUID portfolioId;
    private final UUID instrumentId;
    private final UUID userId;

    /** Monto colocado en este instrumento. */
    private final BigDecimal montoInvertido;

    /** Snapshot de la tasa anual al momento de invertir. */
    private final BigDecimal tasaAplicada;

    /** Snapshot del plazo en días al momento de invertir. */
    private final int plazoDias;

    private final LocalDate fechaInicio;
    private final LocalDate fechaVencimiento;

    /** Rendimiento ganado hasta el momento (actualizado al madurar). */
    private final BigDecimal rendimientoGanado;

    private final InvestmentStatus estado;
    private final Instant canceladoAt;
    private final Instant createdAt;

    /**
     * Calcula el rendimiento simple: capital × tasa × días / 365.
     * Fórmula: montoInvertido * tasaAplicada * plazoDias / 365
     */
    public BigDecimal calcularRendimiento() {
        return montoInvertido
                .multiply(tasaAplicada)
                .multiply(BigDecimal.valueOf(plazoDias))
                .divide(BigDecimal.valueOf(365), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal calcularTotalAlVencer() {
        return montoInvertido.add(calcularRendimiento());
    }

    public boolean estaVencida(LocalDate hoy) {
        return !fechaVencimiento.isAfter(hoy) && estado == InvestmentStatus.ACTIVE;
    }

    public MicroInvestment withEstado(InvestmentStatus nuevoEstado) {
        return toBuilder().estado(nuevoEstado).build();
    }

    public MicroInvestment withRendimientoGanado(BigDecimal rendimiento) {
        return toBuilder().rendimientoGanado(rendimiento).build();
    }

    public MicroInvestment cancelar() {
        return toBuilder()
                .estado(InvestmentStatus.CANCELLED)
                .canceladoAt(Instant.now())
                .build();
    }
}
