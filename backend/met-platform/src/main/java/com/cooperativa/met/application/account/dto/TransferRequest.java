package com.cooperativa.met.application.account.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record TransferRequest(
        @NotNull(message = "Destination account is required")
        UUID destinationAccountId,
        
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        BigDecimal amount,
        
        String concept,
        
        @NotBlank(message = "PIN is required")
        String pin
) {
}
