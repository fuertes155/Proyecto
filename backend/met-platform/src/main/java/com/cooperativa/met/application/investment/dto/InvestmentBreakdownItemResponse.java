package com.cooperativa.met.application.investment.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Una posición real del motor de distribución P2P: a qué deudor (o al fondo
 * de liquidez, si aún no fue emparejada) quedó fraccionado el capital
 * depositado por el inversionista.
 */
public record InvestmentBreakdownItemResponse(
        UUID fractionId,
        String borrowerName,
        UUID loanId,
        BigDecimal amount,
        String status,
        Instant matchedAt
) {
}
