package com.cooperativa.met.domain.admin.model;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * Límite de monto de operación por tipo (TRANSFER, WITHDRAWAL, PURCHASE).
 * Configurable y auditable desde el panel admin.
 */
@Getter
@Builder(toBuilder = true)
public class OperationLimit {

    private final UUID id;
    private final String tipoOperacion;
    private final long montoDiarioMax;
    private final long montoPorTransaccionMax;
    private final boolean activo;
    private final UUID creadoPor;
    private final Instant updatedAt;

    public OperationLimit withLimits(long newDiario, long newPorTransaccion) {
        return this.toBuilder()
                .montoDiarioMax(newDiario)
                .montoPorTransaccionMax(newPorTransaccion)
                .updatedAt(Instant.now())
                .build();
    }
}
