package com.cooperativa.met.application.account.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class GeneratePseLinkRequest {
    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "10000.0", message = "El monto mínimo de depósito es de $10.000 COP")
    private BigDecimal amount;
}
