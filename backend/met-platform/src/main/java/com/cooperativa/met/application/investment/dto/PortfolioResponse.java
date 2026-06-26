package com.cooperativa.met.application.investment.dto;

import com.cooperativa.met.domain.investment.model.MicroInvestmentPortfolio;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Respuesta con el resumen de un portfolio de micro-inversiones,
 * incluyendo todas las posiciones y el rendimiento total proyectado.
 */
public record PortfolioResponse(
        UUID id,
        UUID userId,
        BigDecimal montoTotal,
        String estrategia,
        String estado,
        BigDecimal rendimientoTotalProyectado,
        BigDecimal totalAlVencer,
        Instant createdAt,
        List<MicroInvestmentResponse> posiciones
) {
    public static PortfolioResponse from(
            MicroInvestmentPortfolio portfolio,
            List<MicroInvestmentResponse> posiciones) {

        BigDecimal rendimientoTotal = posiciones.stream()
                .map(MicroInvestmentResponse::rendimientoProyectado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new PortfolioResponse(
                portfolio.getId(),
                portfolio.getUserId(),
                portfolio.getMontoTotal(),
                portfolio.getEstrategia().name(),
                portfolio.getEstado().name(),
                rendimientoTotal,
                portfolio.getMontoTotal().add(rendimientoTotal),
                portfolio.getCreatedAt(),
                posiciones
        );
    }
}
