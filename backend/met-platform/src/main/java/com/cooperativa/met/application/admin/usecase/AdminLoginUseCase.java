package com.cooperativa.met.application.admin.usecase;

import com.cooperativa.met.application.admin.dto.AdminAuthResponse;
import com.cooperativa.met.application.admin.dto.AdminLoginRequest;
import com.cooperativa.met.domain.admin.model.Admin;
import com.cooperativa.met.domain.admin.model.AdminAuditEntry;
import com.cooperativa.met.domain.admin.port.AdminAuditLogPort;
import com.cooperativa.met.domain.admin.port.AdminRepositoryPort;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.common.exception.ResourceNotFoundException;
import com.cooperativa.met.domain.identity.port.TokenPort;
import com.cooperativa.met.infrastructure.config.MetSecurityProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AdminLoginUseCase {

    private final AdminRepositoryPort adminRepository;
    private final AdminAuditLogPort auditLog;
    private final TokenPort tokenPort;
    private final PasswordEncoder passwordEncoder;
    private final MetSecurityProperties securityProperties;

    public AdminAuthResponse execute(AdminLoginRequest request, String ipOrigen) {
        Admin admin = adminRepository.findByUsername(request.username())
                .orElseThrow(() -> new ResourceNotFoundException("Credenciales inválidas"));

        if (!admin.isActive()) {
            throw new BusinessRuleException("ADMIN_SUSPENDED", "La cuenta de administrador está suspendida");
        }

        if (!passwordEncoder.matches(request.password(), admin.getPasswordHash())) {
            throw new BusinessRuleException("INVALID_CREDENTIALS", "Credenciales inválidas");
        }

        String accessToken = tokenPort.generateAdminAccessToken(
                admin.getId(), admin.getUsername(), admin.getRole().name());

        auditLog.log(AdminAuditEntry.builder()
                .actorAdminId(admin.getId())
                .accion("ADMIN_LOGIN")
                .entidadAfectada("ADMIN")
                .idEntidad(admin.getId().toString())
                .motivo("Inicio de sesión administrativo")
                .ipOrigen(ipOrigen)
                .timestamp(Instant.now())
                .build());

        return AdminAuthResponse.of(
                admin.getId(),
                admin.getUsername(),
                admin.getFullName(),
                admin.getRole().name(),
                accessToken,
                securityProperties.getJwt().getExpirationMs()
        );
    }
} // Force language server sync
