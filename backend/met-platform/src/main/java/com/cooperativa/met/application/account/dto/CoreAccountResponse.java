package com.cooperativa.met.application.account.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CoreAccountResponse(
        UUID id,
        String accountNumber,
        BigDecimal balance,
        String status
) {
}
