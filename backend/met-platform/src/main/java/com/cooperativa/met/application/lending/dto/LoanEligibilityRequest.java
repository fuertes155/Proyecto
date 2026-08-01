package com.cooperativa.met.application.lending.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record LoanEligibilityRequest(
        @NotNull(message = "Debe enviar la confirmación de términos")
        @AssertTrue(message = "Debes aceptar los términos de Habeas Data")
        Boolean acceptedHabeasData
) {
}
