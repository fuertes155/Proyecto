package com.cooperativa.met.domain.investment.port;

import com.cooperativa.met.domain.investment.model.GuaranteeFundMovement;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface GuaranteeFundPort {

    GuaranteeFundMovement save(GuaranteeFundMovement movement);

    /** Saldo actual del fondo: suma de CONTRIBUTION menos suma de PAYOUT. */
    BigDecimal getBalance();

    List<GuaranteeFundMovement> findByTransactionReference(UUID transactionReference);
}
