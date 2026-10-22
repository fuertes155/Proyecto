package com.cooperativa.met.application.identity.usecase;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cooperativa.met.application.identity.dto.AuthResponse;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.identity.model.RefreshToken;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.model.UserStatus;
import com.cooperativa.met.domain.identity.port.RefreshTokenRepositoryPort;
import com.cooperativa.met.domain.identity.port.TokenPort;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import com.cooperativa.met.infrastructure.config.MetSecurityProperties;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenUseCase {

    private final TokenPort tokenPort;
    private final RefreshTokenRepositoryPort refreshTokenRepository;
    private final UserRepositoryPort userRepository;
    private final MetSecurityProperties securityProperties;

    // noRollbackFor: varios rechazos revocan sesiones y justo después lanzan la excepción. Con el
    // rollback por defecto esa revocación se deshacía y las sesiones quedaban abiertas.
    @Transactional(noRollbackFor = BusinessRuleException.class)
    public AuthResponse execute(String refreshToken) {
        TokenPort.RefreshTokenClaims claims = tokenPort.validateRefreshTokenClaims(refreshToken);

        UUID userId = claims.userId();
        UUID jti = claims.jti();
        if (jti == null) {
            throw new BusinessRuleException("INVALID_TOKEN", "Refresh token sin jti");
        }

        RefreshToken persisted = refreshTokenRepository.findByJti(jti)
                .orElseThrow(() -> new BusinessRuleException("INVALID_TOKEN", "Refresh token no registrado"));

        if (persisted.revoked()) {
            // DETECCIÓN DE REÚSO: Si se intenta usar un token ya revocado (usado),
            // asumimos que la sesión fue clonada/robada y cerramos todas las sesiones.
            refreshTokenRepository.revokeAllByUserId(userId);
            throw new BusinessRuleException("REFRESH_TOKEN_REUSED", "Se ha detectado un uso anómalo de la sesión. Por seguridad, todas las sesiones han sido cerradas.");
        }
        if (persisted.expiresAt().isBefore(Instant.now())) {
            throw new BusinessRuleException("INVALID_TOKEN", "Refresh token expirado");
        }

        // Revocar token actual (rotación)
        refreshTokenRepository.revoke(jti);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessRuleException("INVALID_TOKEN", "Usuario inválido"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            // El login ya rechaza cuentas no activas; sin esto, un usuario bloqueado (lista
            // restrictiva, bloqueo de emergencia) seguiría renovando su sesión indefinidamente.
            refreshTokenRepository.revokeAllByUserId(userId);
            throw new BusinessRuleException("USER_NOT_ACTIVE", "La cuenta no está activa");
        }

        String newAccessToken = tokenPort.generateAccessToken(userId, null);
        String newRefreshToken = tokenPort.generateRefreshToken(userId);

        // Persistir el nuevo refresh
        TokenPort.RefreshTokenClaims newClaims = tokenPort.validateRefreshTokenClaims(newRefreshToken);
        UUID newJti = newClaims.jti();
        Instant now = Instant.now();
        Instant newExpiresAt = now.plusMillis(securityProperties.getJwt().getRefreshExpirationMs());

        RefreshToken newEntity = new RefreshToken(
                newJti,
                userId,
                now,
                newExpiresAt,
                false
        );
        refreshTokenRepository.save(newEntity);

        return AuthResponse.of(
                userId,
                newAccessToken,
                newRefreshToken,
                securityProperties.getJwt().getExpirationMs()
        );
    }
}
