package com.cooperativa.met.application.admin.usecase;

import com.cooperativa.met.application.admin.dto.EmergencyLockRequest;
import com.cooperativa.met.domain.admin.model.AdminAuditEntry;
import com.cooperativa.met.domain.admin.port.AdminAuditLogPort;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.common.exception.ResourceNotFoundException;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.model.UserStatus;
import com.cooperativa.met.domain.identity.port.RefreshTokenRepositoryPort;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class EmergencyLockUseCase {

    private final UserRepositoryPort userRepository;
    private final RefreshTokenRepositoryPort refreshTokenRepository;
    private final AdminAuditLogPort auditLog;
    private final ObjectMapper objectMapper;

    @Transactional
    public void execute(UUID actorAdminId, EmergencyLockRequest request, String ipOrigen) {
        String scope = request.scope().toUpperCase();

        switch (scope) {
            case "USER" -> lockUser(actorAdminId, request, ipOrigen);
            case "ACCESS_TOKEN" -> revokeAllTokens(actorAdminId, request, ipOrigen);
            default -> throw new BusinessRuleException("INVALID_SCOPE",
                    "Alcance inválido. Use: USER, ACCESS_TOKEN");
        }
    }

    private void lockUser(UUID actorAdminId, EmergencyLockRequest request, String ip) {
        UUID userId = parseUUID(request.targetId());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + request.targetId()));

        String prevStatus = user.getStatus().name();
        User blocked = user.withStatus(UserStatus.SUSPENDED);
        userRepository.save(blocked);

        // Revocar todos los tokens activos del usuario
        refreshTokenRepository.revokeAllByUserId(userId);

        auditLog.log(buildAuditEntry(actorAdminId, "EMERGENCY_LOCK_USER", "USER",
                request.targetId(), Map.of("status", prevStatus),
                Map.of("status", UserStatus.SUSPENDED.name()), request.reason(), ip));
    }

    private void revokeAllTokens(UUID actorAdminId, EmergencyLockRequest request, String ip) {
        UUID userId = parseUUID(request.targetId());
        refreshTokenRepository.revokeAllByUserId(userId);

        auditLog.log(buildAuditEntry(actorAdminId, "EMERGENCY_REVOKE_TOKENS", "USER",
                request.targetId(), null,
                Map.of("action", "ALL_TOKENS_REVOKED"), request.reason(), ip));
    }

    private UUID parseUUID(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException("INVALID_ID", "ID inválido: " + id);
        }
    }

    private AdminAuditEntry buildAuditEntry(UUID actorId, String accion, String entidad,
                                             String idEntidad, Map<String, Object> prev,
                                             Map<String, Object> next, String motivo, String ip) {
        return AdminAuditEntry.builder()
                .actorAdminId(actorId)
                .accion(accion)
                .entidadAfectada(entidad)
                .idEntidad(idEntidad)
                .valoresAnteriores(toJson(prev))
                .valoresNuevos(toJson(next))
                .motivo(motivo)
                .ipOrigen(ip)
                .timestamp(Instant.now())
                .build();
    }

    private String toJson(Map<String, Object> map) {
        if (map == null) return null;
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }
}
