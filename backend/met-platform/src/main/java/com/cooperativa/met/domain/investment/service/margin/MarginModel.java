package com.cooperativa.met.domain.investment.service.margin;

/**
 * Modelo de cálculo del margen de la plataforma sobre el interés que paga el deudor.
 * Configurable vía {@code met.capital-engine.margin-model} — no se debe hardcodear
 * un modelo en el código de negocio, se resuelve siempre a través de {@link MarginStrategy#of(MarginModel)}.
 */
public enum MarginModel {

    /**
     * Modelo A — Resta directa de tasas: r_i = r_d - m.
     * Nota: aplicado periodo a periodo sobre el mismo capital base, es matemáticamente
     * equivalente a CAPITAL_COMMISSION (ambos restan capitalBase × m del interés generado).
     * Se mantiene como estrategia independiente porque la forma de disclosure al
     * inversionista/contrato es distinta (spread de tasa vs. comisión sobre capital).
     */
    DIRECT_RATE_SUBTRACTION,

    /**
     * Modelo B — Comisión sobre el interés generado (recomendado, estándar P2P).
     * Comisión = Interés_generado × m. Escala con lo que realmente se generó.
     */
    INTEREST_COMMISSION,

    /**
     * Modelo C — Comisión sobre el capital.
     * Comisión = Capital × m, independiente del interés generado en el periodo.
     */
    CAPITAL_COMMISSION
}
