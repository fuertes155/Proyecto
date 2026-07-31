package com.cooperativa.met.domain.investment.model;

/**
 * Categoría del movimiento contable, independiente de la dirección (DEBIT/CREDIT).
 * Permite reportar y auditar cuánto del dinero movido corresponde a capital,
 * rendimiento, comisión de plataforma o Fondo de Garantías.
 */
public enum LedgerEntryCategory {
    /** Dispersión inicial de capital del inversionista hacia el deudor. */
    FUNDING_DISBURSEMENT,
    /** Devolución de capital al inversionista al pagarse una cuota. */
    PRINCIPAL_REPAYMENT,
    /** Rendimiento neto (ya descontado el margen) acreditado al inversionista. */
    INTEREST_YIELD,
    /** Comisión retenida por la plataforma sobre el interés de una cuota. */
    PLATFORM_FEE,
    /** Aporte de dinero al Fondo de Garantías/Aval Colectivo. */
    GUARANTEE_FUND_CONTRIBUTION,
    /** Desembolso del Fondo de Garantías para cubrir (parcialmente) una mora. */
    GUARANTEE_FUND_PAYOUT,
    /** Movimientos anteriores a la introducción de esta categoría o sin clasificar. */
    OTHER
}
