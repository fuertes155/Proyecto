package com.cooperativa.met.domain.bank.port;

/**
 * Representa un banco tal como lo reporta el proveedor del riel de pago
 * (ej. GET /banks de Wompi). Se usa para sincronizar el catálogo interno
 * {@code banks} — el dominio nunca inventa estos códigos, solo los refleja.
 */
public record PayoutGatewayBank(String providerBankId, String name, boolean supportsPayout) {
}
