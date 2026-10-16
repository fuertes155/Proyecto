package com.cooperativa.met.application.admin.dto;

import com.cooperativa.met.domain.identity.model.DocumentType;
import com.cooperativa.met.domain.identity.model.KycStatus;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.model.UserStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Socio tal como lo ve el panel administrativo. Antes se devolvía el {@link User} de dominio
 * completo, que incluye el hash del PIN, el hash biométrico, el token de la tarjeta de pago,
 * la IP y el dispositivo: un PIN de 4 dígitos se recupera de su hash por fuerza bruta en
 * minutos, así que nada de eso puede salir hacia el navegador del administrador.
 */
public record AdminUserResponse(
        UUID id,
        DocumentType documentType,
        String documentNumber,
        String firstName,
        String lastName,
        String email,
        String phone,
        UserStatus status,
        KycStatus kycStatus,
        Instant createdAt
) {
    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getDocumentType(),
                user.getDocumentNumber(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                user.getStatus(),
                user.getKycStatus(),
                user.getCreatedAt());
    }
}
