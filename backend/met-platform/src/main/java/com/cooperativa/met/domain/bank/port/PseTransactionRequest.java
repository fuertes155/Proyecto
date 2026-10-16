package com.cooperativa.met.domain.bank.port;

import java.math.BigDecimal;

/**
 * @param customerFullName nombre del pagador; Wompi lo pide para PSE ({@code customer_data.full_name}).
 * @param customerPhone    celular del pagador, puede ser null ({@code customer_data.phone_number}).
 */
public record PseTransactionRequest(
        String reference,
        BigDecimal amount,
        String customerEmail,
        String customerFullName,
        String customerPhone,
        String documentType,
        String documentNumber,
        String financialInstitutionCode,
        String redirectUrl,
        String description
) {
}
