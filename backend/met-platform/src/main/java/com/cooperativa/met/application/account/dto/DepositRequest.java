package com.cooperativa.met.application.account.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DepositRequest {
    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "1.0", message = "El monto debe ser mayor a 0")
    private BigDecimal amount;

    @NotBlank(message = "El método de recarga es obligatorio")
    private String method;

    private String reference;
}
