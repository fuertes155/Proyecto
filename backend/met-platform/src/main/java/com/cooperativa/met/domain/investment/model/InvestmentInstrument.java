package com.cooperativa.met.domain.investment.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Instrumento de inversión disponible en la plataforma.
 * Administrado por el panel de admin (tasa, plazo, mínimo, cupo).
 */
@Getter
@Builder(toBuilder = true)
public class InvestmentInstrument {

    private final UUID id;
    private final String nombre;
    private final String descripcion;

    /** Tasa de interés nominal anual. Ej: 0.085 = 8.5% */
    private final BigDecimal tasaAnual;

    /** Duración de la inversión en días. */
    private final int plazoDias;

    /** Monto mínimo que se puede invertir en este instrumento. */
    private final BigDecimal montoMinimo;

    /** Cupo máximo del instrumento. Null = ilimitado. */
    private final BigDecimal cupoMaximo;

    private final boolean activo;
    private final UUID creadoPor;
    private final Instant createdAt;
    private final Instant updatedAt;

    public InvestmentInstrument withActivo(boolean nuevoEstado) {
        return toBuilder().activo(nuevoEstado).updatedAt(Instant.now()).build();
    }

    public InvestmentInstrument withTasa(BigDecimal nuevaTasa) {
        return toBuilder().tasaAnual(nuevaTasa).updatedAt(Instant.now()).build();
    }
}
