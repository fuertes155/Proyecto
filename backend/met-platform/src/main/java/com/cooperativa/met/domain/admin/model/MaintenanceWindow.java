package com.cooperativa.met.domain.admin.model;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Ventana de mantenimiento programado.
 * Cuando está activa, el sistema puede rechazar ciertas peticiones.
 * endpointsActivos: lista de rutas que siguen operativas durante el mantenimiento.
 */
@Getter
@Builder(toBuilder = true)
public class MaintenanceWindow {

    private final UUID id;
    private final String descripcion;
    private final Instant inicio;
    private final Instant fin;
    private final boolean activo;
    private final List<String> endpointsActivos;
    private final UUID creadoPor;
    private final Instant createdAt;

    public MaintenanceWindow activate() {
        return this.toBuilder().activo(true).build();
    }

    public MaintenanceWindow deactivate() {
        return this.toBuilder().activo(false).build();
    }

    public boolean isEnCurso() {
        Instant now = Instant.now();
        return activo && !now.isBefore(inicio) && now.isBefore(fin);
    }
}
