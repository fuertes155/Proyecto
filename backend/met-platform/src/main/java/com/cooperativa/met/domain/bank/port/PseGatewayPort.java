package com.cooperativa.met.domain.bank.port;

import java.util.List;

/**
 * Puerto hacia el producto de Checkout/Transacciones de Wompi para crear
 * pagos PSE nativos (sin redirigir al checkout hosteado). Distinto de
 * {@link PayoutGatewayPort}: es otro producto de Wompi, con sus propias
 * credenciales (public-key/private-key ya usadas hoy por el checkout
 * hosteado) y su propio namespace de códigos de banco.
 */
public interface PseGatewayPort {

    List<PseFinancialInstitution> fetchFinancialInstitutions();

    PseTransactionResult createPseTransaction(PseTransactionRequest request);
}
