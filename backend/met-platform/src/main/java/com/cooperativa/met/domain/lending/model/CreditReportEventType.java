package com.cooperativa.met.domain.lending.model;

/**
 * Eventos de "vida crediticia" que la cooperativa, como fuente de información,
 * está obligada a reportar periódicamente a la central de riesgo (Ley 1266 de 2008).
 */
public enum CreditReportEventType {
    /** El crédito está vigente y al día en sus pagos. */
    AL_DIA,
    /** El crédito tiene cuotas en mora (reportado a partir del día 6, con preaviso previo al deudor). */
    MORA,
    /** El crédito fue pagado en su totalidad. */
    PAGADO
}
