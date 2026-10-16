package com.cooperativa.met.application.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Activa el ingreso con huella en el celular del socio.
 *
 * @param pin            PIN actual cifrado con RSA (igual que en el cambio de PIN): confirma que quien
 *                       activa la huella es el titular y no alguien con el celular desbloqueado.
 * @param biometricToken llave aleatoria generada y guardada en el celular. El servidor solo guarda su
 *                       hash; el celular la envía en el login después de que el socio pone la huella.
 */
public record EnableBiometricLoginRequest(
        @NotBlank String pin,
        @NotBlank @Size(min = 32, max = 256) String biometricToken
) {
}
