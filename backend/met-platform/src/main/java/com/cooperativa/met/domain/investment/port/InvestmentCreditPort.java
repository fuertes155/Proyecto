package com.cooperativa.met.domain.investment.port;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Puerto para acreditar fondos al saldo principal del usuario.
 * Se usa al madurar una inversión (capital + rendimiento)
 * o al cancelar anticipadamente (solo capital).
 */
public interface InvestmentCreditPort {

    /**
     * Acredita el monto al saldo del usuario.
     *
     * @param userId    ID del usuario receptor.
     * @param amount    Monto a acreditar.
     * @param reference Referencia de la transacción.
     * @return true si el crédito fue exitoso.
     */
    boolean credit(UUID userId, BigDecimal amount, String reference);
}
