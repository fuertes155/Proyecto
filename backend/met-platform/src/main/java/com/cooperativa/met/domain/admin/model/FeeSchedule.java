package com.cooperativa.met.domain.admin.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Tarifa con versionado temporal.
 * Se gestiona como "vigente_desde / vigente_hasta" para preservar historial.
 */
@Getter
@Builder
public class FeeSchedule {

    private final UUID id;
    private final String tipoTarifa;
    private final String descripcion;
    private final BigDecimal valor;
    private final boolean esPorcentaje;
    private final Instant vigentDesde;
    private final Instant vigentaHasta;
    private final UUID creadoPor;
    private final Instant createdAt;

    public boolean isVigente() {
        Instant now = Instant.now();
        boolean afterStart = !now.isBefore(vigentDesde);
        boolean beforeEnd = vigentaHasta == null || now.isBefore(vigentaHasta);
        return afterStart && beforeEnd;
    }
}
