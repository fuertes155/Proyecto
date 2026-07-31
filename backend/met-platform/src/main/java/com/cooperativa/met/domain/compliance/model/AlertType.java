package com.cooperativa.met.domain.compliance.model;

/** Tipo de patrón inusual que disparó una alerta SARLAFT. */
public enum AlertType {
    /** Monto muy por encima del umbral absoluto o del promedio histórico del usuario. */
    UNUSUAL_AMOUNT,
    /** Demasiadas operaciones en una ventana de 24 horas. */
    UNUSUAL_FREQUENCY,
    /** Varias operaciones seguidas justo por debajo del umbral de reporte (fraccionamiento/pitufeo). */
    STRUCTURING_PATTERN,
    /** Depósito seguido casi de inmediato por un retiro/transferencia de monto similar. */
    RAPID_IN_OUT
}
