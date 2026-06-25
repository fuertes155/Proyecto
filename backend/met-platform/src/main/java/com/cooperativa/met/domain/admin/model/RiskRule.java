package com.cooperativa.met.domain.admin.model;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * Regla de riesgo automatizada.
 * La condición es un JSON que el motor de riesgo evalúa.
 * La acción define qué hacer al dispararse: ALLOW, BLOCK o REVIEW.
 */
@Getter
@Builder(toBuilder = true)
public class RiskRule {

    private final UUID id;
    private final String nombre;
    private final String descripcion;
    /** JSON: {"field":"amount","operator":"gt","value":10000000} */
    private final String condicion;
    private final RiskAction accion;
    private final boolean activo;
    private final UUID creadoPor;
    private final Instant createdAt;

    public RiskRule withActivo(boolean nuevoEstado) {
        return this.toBuilder().activo(nuevoEstado).build();
    }
}
