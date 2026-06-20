package com.cooperativa.met.application.identity.usecase;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cooperativa.met.application.identity.dto.AuthResponse;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.identity.model.RefreshToken;
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

    @Transactional
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
            throw new BusinessRuleException("REFRESH_TOKEN_REVOKED", "Refresh token revocado");
        }
        if (persisted.expiresAt().isBefore(Instant.now())) {
            throw new BusinessRuleException("INVALID_TOKEN", "Refresh token expirado");
        }

        // Revocar token actual (rotación)
        refreshTokenRepository.revoke(jti);

        // (opcional) verificar que el usuario existe/está activo
        userRepository.findById(userId)
                .orElseThrow(() -> new BusinessRuleException("INVALID_TOKEN", "Usuario inválido"));

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
