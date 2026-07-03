package com.cooperativa.met.application.identity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdatePinRequest {
    @NotBlank(message = "El PIN actual es obligatorio")
    private String currentPin;

    @NotBlank(message = "El nuevo PIN es obligatorio")
    private String newPin;
}
