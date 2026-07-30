package com.cooperativa.met.domain.bank.port;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Instrucción de payout hacia el riel externo. El nombre/identificación
 * del beneficiario siempre proviene de la identidad KYC del usuario dueño
 * de la wallet (nunca de un dato que el usuario pueda editar libremente),
 * para garantizar que el destino sea siempre una cuenta propia.
 */
public record PayoutGatewayRequest(
        String reference,
        BigDecimal amount,
        String destinationWompiBankId,
        String destinationAccountType,
        String destinationAccountNumber,
        String beneficiaryDocumentType,
        String beneficiaryDocumentNumber,
        String beneficiaryFullName,
        String beneficiaryEmail,
        UUID internalPayoutId
) {
}
