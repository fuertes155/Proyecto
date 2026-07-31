package com.cooperativa.met.domain.investment.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Movimiento del Fondo de Garantías/Aval Colectivo. El saldo del fondo es la
 * suma de sus movimientos (CONTRIBUTION suma, PAYOUT resta) — es un libro de
 * eventos, igual que {@link LedgerEntry}, no un saldo mutable directo.
 */
@Getter
@Builder
public class GuaranteeFundMovement {
    private final UUID id;
    private final GuaranteeFundMovementType type;
    private final BigDecimal amount;
    private final UUID transactionReference;
    private final String concept;
    private final Instant createdAt;
}
