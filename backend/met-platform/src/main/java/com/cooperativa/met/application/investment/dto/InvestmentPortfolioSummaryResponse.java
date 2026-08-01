package com.cooperativa.met.application.investment.dto;

import java.math.BigDecimal;

/**
 * Vista del inversionista sobre su propio capital fraccionado por el motor P2P.
 * A propósito NO incluye a qué deudores específicos quedó emparejado cada
 * fracción: esa identidad es información sensible de otro socio y solo debe
 * ser visible para administradores (ver AdminInvestmentController).
 */
public record InvestmentPortfolioSummaryResponse(
        BigDecimal totalInvested,
        BigDecimal activeAmount,
        BigDecimal availableAmount,
        BigDecimal paidOffAmount,
        BigDecimal returnedAmount,
        int loansFundedCount
) {
}
