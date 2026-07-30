package com.cooperativa.met.domain.bank.port;

/**
 * @param asyncPaymentUrl URL a la que se redirige al usuario para autenticarse
 *                        en su banco y completar el pago PSE.
 */
public record PseTransactionResult(String transactionId, String asyncPaymentUrl, String status) {
}
