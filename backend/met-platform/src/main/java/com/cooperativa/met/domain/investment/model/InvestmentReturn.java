package com.cooperativa.met.domain.investment.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Registro de pago de rendimiento al vencer una posición de micro-inversión.
 * Representa el crédito de capital + rendimiento al saldo del usuario.
 */
@Getter
@Builder
public class InvestmentReturn {

    private final UUID id;
    private final UUID investmentId;
    private final UUID userId;

    /** Capital original devuelto. */
    private final BigDecimal capital;

    /** Rendimiento ganado por el plazo. */
    private final BigDecimal rendimiento;

    /** capital + rendimiento (monto total acreditado al usuario). */
    private final BigDecimal totalAcreditado;

    private final LocalDate fechaPago;
    private final Instant createdAt;
}
