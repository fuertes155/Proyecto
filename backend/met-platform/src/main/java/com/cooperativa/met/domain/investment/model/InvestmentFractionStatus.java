package com.cooperativa.met.domain.investment.model;

public enum InvestmentFractionStatus {
    AVAILABLE,
    MATCHED,
    RETURNED,
    /** Se dividió en piezas más pequeñas (parte MATCHED a distintos deudores, parte AVAILABLE). */
    SPLIT
}
