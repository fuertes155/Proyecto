package com.cooperativa.met.domain.lending.model;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * Value Object que representa el resultado de una consulta
 * a una central de riesgo crediticio (ej. DataCrédito Experian).
 *
 * En DataCrédito, el score suele tener una escala diferente (ej. 150 a 950).
 * Valores típicos (aproximados):
 *   < 600 → Alto riesgo (rechazado automáticamente)
 *   600–749 → Riesgo medio (aprobado con revisión)
 *   750+ → Bajo riesgo (aprobado)
 */
@Getter
@Builder
public class CreditScoreResult {

    /** Score crediticio (escala de la central). */
    private final int score;

    /** Nombre del proveedor: "DATACREDITO", "MOCK", etc. */
    private final String provider;

    /**
     * Identificador de referencia externo de la consulta.
     * Permite trazar la respuesta en los sistemas de Experian.
     * Puede ser null cuando el proveedor es MOCK.
     */
    private final String referenceId;

    /** Timestamp exacto en que se realizó la consulta. */
    private final Instant queriedAt;
}
