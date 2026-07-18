package com.cooperativa.met.application.identity.usecase;

import com.cooperativa.met.domain.identity.port.RefreshTokenRepositoryPort;
import com.cooperativa.met.domain.identity.port.TokenPort;
import com.cooperativa.met.infrastructure.config.MetSecurityProperties;
import com.cooperativa.met.infrastructure.security.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * Caso de uso de cierre de sesión seguro.
 *
 * <ol>
 *   <li>Agrega el access token a la lista negra en Redis (expira automáticamente)</li>
 *   <li>Revoca todos los refresh tokens del usuario en base de datos</li>
 * </ol>
 *
 * <p>Así, aunque alguien hubiera robado un token, queda inutilizable inmediatamente.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogoutUseCase {

    private final TokenBlacklistService tokenBlacklist;
    private final RefreshTokenRepositoryPort refreshTokenRepository;
    private final MetSecurityProperties securityProperties;

    public void execute(UUID userId, String accessToken) {
        // 1. Calcular tiempo restante del access token y añadirlo a la lista negra
        long expirationMs = securityProperties.getJwt().getExpirationMs();
        Duration ttl = Duration.ofMillis(expirationMs);
        tokenBlacklist.revoke(accessToken, ttl);

        // 2. Revocar todos los refresh tokens del usuario
        try {
            refreshTokenRepository.revokeAllByUserId(userId);
            log.info("Logout exitoso. userId={} tokens revocados.", userId);
        } catch (Exception e) {
            log.warn("Error al revocar refresh tokens en logout. userId={}: {}", userId, e.getMessage());
        }
    }
}
