package com.cooperativa.met.domain.admin.model;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * Registro inmutable de una acción administrativa.
 * Cada acción del panel admin genera una entrada aquí.
 */
@Getter
@Builder
public class AdminAuditEntry {

    private final UUID id;
    private final UUID actorAdminId;
    private final String accion;
    private final String entidadAfectada;
    private final String idEntidad;
    /** JSON serializado de los valores antes del cambio */
    private final String valoresAnteriores;
    /** JSON serializado de los valores después del cambio */
    private final String valoresNuevos;
    private final String motivo;
    private final String ipOrigen;
    private final Instant timestamp;
}
