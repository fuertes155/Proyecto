package com.cooperativa.met.application.lending.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record SubmitLoanApplicationRequest(
        @NotNull(message = "El monto solicitado es obligatorio") 
        @DecimalMin(value = "500000", message = "El monto mínimo es 500,000") 
        @DecimalMax(value = "60000000", message = "El monto máximo es 60,000,000")
        BigDecimal amount,
        
        @NotNull(message = "El plazo en meses es obligatorio") 
        @Min(value = 6, message = "El plazo mínimo es de 6 meses") 
        @Max(value = 60, message = "El plazo máximo es de 60 meses") 
        Integer termMonths,
        
        @NotBlank(message = "El propósito del crédito no puede estar vacío") 
        @Size(max = 255, message = "El propósito no puede exceder los 255 caracteres") 
        String purpose,
        
        @NotNull(message = "Debe enviar la confirmación de términos") 
        @AssertTrue(message = "Debes aceptar los términos de Habeas Data") 
        Boolean hasAcceptedHabeasData
) {
}
