package com.cooperativa.met.application.account.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateNativePseDepositRequest {
    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "10000.0", message = "El monto mínimo de depósito es de $10.000 COP")
    @DecimalMax(value = "100000000.00", message = "El monto máximo por recarga es de $100.000.000 COP")
    private BigDecimal amount;

    @NotBlank(message = "El banco es obligatorio")
    private String bankCode;

    private String returnUrl;
}
