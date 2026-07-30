package com.cooperativa.met.domain.bank.port;

import java.util.List;

/**
 * Puerto hacia el riel de pagos de salida (payout) hacia bancos externos.
 * La implementación actual es {@code WompiPayoutAdapter} ("Pagos a Terceros"
 * de Wompi), pero cualquier caso de uso del dominio depende solo de esta
 * interfaz — si en el futuro se necesita otro proveedor (BaaS/ACH directo),
 * se sustituye el adaptador sin tocar el dominio ni los casos de uso.
 */
public interface PayoutGatewayPort {

    List<PayoutGatewayBank> fetchSupportedBanks();

    PayoutGatewayResult initiatePayout(PayoutGatewayRequest request);
}
