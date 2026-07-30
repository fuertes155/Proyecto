package com.cooperativa.met.domain.bank.port;

import java.math.BigDecimal;

public record PseTransactionRequest(
        String reference,
        BigDecimal amount,
        String customerEmail,
        String documentType,
        String documentNumber,
        String financialInstitutionCode,
        String redirectUrl,
        String description
) {
}
