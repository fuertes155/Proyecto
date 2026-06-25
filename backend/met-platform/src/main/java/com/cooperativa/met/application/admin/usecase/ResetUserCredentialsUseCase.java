package com.cooperativa.met.application.admin.usecase;

import com.cooperativa.met.application.admin.dto.ResetCredentialsRequest;
import com.cooperativa.met.domain.admin.model.AdminAuditEntry;
import com.cooperativa.met.domain.admin.port.AdminAuditLogPort;
import com.cooperativa.met.domain.common.exception.ResourceNotFoundException;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.port.RefreshTokenRepositoryPort;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Reseteo de credenciales de un usuario:
 * 1. Revoca todos sus refresh tokens activos.
 * 2. Limpia el PIN hash (fuerza re-registro de PIN en próximo login).
 * 3. Limpia el biometric hash (fuerza re-registro biométrico).
 * 4. Registra todo en auditoría.
 */
@Service
@RequiredArgsConstructor
public class ResetUserCredentialsUseCase {

    private final UserRepositoryPort userRepository;
    private final RefreshTokenRepositoryPort refreshTokenRepository;
    private final AdminAuditLogPort auditLog;

    @Transactional
    public void execute(UUID actorAdminId, UUID userId, ResetCredentialsRequest request, String ip) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + userId));

        // Revocar todos los tokens activos
        refreshTokenRepository.revokeAllByUserId(userId);

        // Limpiar PIN y biometría — fuerza re-registro
        User reset = user.toBuilder()
                .pinHash(null)
                .biometricHash(null)
                .updatedAt(Instant.now())
                .build();
        userRepository.save(reset);

        auditLog.log(AdminAuditEntry.builder()
                .actorAdminId(actorAdminId)
                .accion("USER_CREDENTIALS_RESET")
                .entidadAfectada("USER")
                .idEntidad(userId.toString())
                .valoresAnteriores("{\"pinHash\":\"[REDACTED]\",\"biometricHash\":\"[REDACTED]\"}")
                .valoresNuevos("{\"pinHash\":null,\"biometricHash\":null,\"tokensRevoked\":true}")
                .motivo(request.reason())
                .ipOrigen(ip)
                .timestamp(Instant.now())
                .build());
    }
}
