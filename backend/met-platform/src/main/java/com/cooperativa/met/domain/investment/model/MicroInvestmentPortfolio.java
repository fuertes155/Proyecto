package com.cooperativa.met.domain.investment.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Portfolio de micro-inversiones de un usuario.
 * Agrupa todas las posiciones individuales creadas en una distribución.
 */
@Getter
@Builder(toBuilder = true)
public class MicroInvestmentPortfolio {

    private final UUID id;
    private final UUID userId;
    private final BigDecimal montoTotal;
    private final InvestmentStrategy estrategia;
    private final InvestmentStatus estado;
    private final Instant createdAt;
    private final Instant updatedAt;

    public MicroInvestmentPortfolio withEstado(InvestmentStatus nuevoEstado) {
        return toBuilder().estado(nuevoEstado).updatedAt(Instant.now()).build();
    }
}
