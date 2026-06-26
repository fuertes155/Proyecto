package com.cooperativa.met.application.investment.dto;

import com.cooperativa.met.domain.investment.model.MicroInvestment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Respuesta con los datos de una posición individual de micro-inversión,
 * incluyendo el rendimiento proyectado al vencimiento.
 */
public record MicroInvestmentResponse(
        UUID id,
        UUID portfolioId,
        UUID instrumentId,
        String instrumentNombre,
        BigDecimal montoInvertido,
        BigDecimal tasaAplicada,
        int plazoDias,
        LocalDate fechaInicio,
        LocalDate fechaVencimiento,
        BigDecimal rendimientoProyectado,
        BigDecimal totalAlVencer,
        BigDecimal rendimientoGanado,
        String estado
) {
    public static MicroInvestmentResponse from(MicroInvestment inv, String instrumentNombre) {
        return new MicroInvestmentResponse(
                inv.getId(),
                inv.getPortfolioId(),
                inv.getInstrumentId(),
                instrumentNombre,
                inv.getMontoInvertido(),
                inv.getTasaAplicada(),
                inv.getPlazoDias(),
                inv.getFechaInicio(),
                inv.getFechaVencimiento(),
                inv.calcularRendimiento(),
                inv.calcularTotalAlVencer(),
                inv.getRendimientoGanado(),
                inv.getEstado().name()
        );
    }
}
