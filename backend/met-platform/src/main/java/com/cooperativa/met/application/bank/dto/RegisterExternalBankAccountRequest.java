package com.cooperativa.met.application.bank.dto;

import com.cooperativa.met.domain.bank.model.BankAccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record RegisterExternalBankAccountRequest(
        @NotBlank(message = "El banco es obligatorio")
        String bankCode,

        @NotNull(message = "El tipo de cuenta es obligatorio")
        BankAccountType accountType,

        // Wompi exige número de cuenta solo numérico, 6 a 20 caracteres
        @NotBlank(message = "El número de cuenta es obligatorio")
        @Pattern(regexp = "^[0-9]{6,20}$", message = "El número de cuenta debe tener entre 6 y 20 dígitos")
        String accountNumber
) {
}
