package com.cooperativa.met.application.identity.usecase;

import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.identity.model.RefreshToken;
import com.cooperativa.met.domain.identity.port.RefreshTokenRepositoryPort;
import com.cooperativa.met.domain.identity.port.TokenPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Con base de datos real (H2) porque lo que se prueba es la transacción: el caso de uso lanza
 * una excepción justo después de revocar las sesiones, y esa revocación tiene que quedar
 * guardada igual. Un test con mocks no puede ver un rollback.
 */
@SpringBootTest
@org.springframework.test.context.ActiveProfiles("test")
@org.springframework.context.annotation.Import(com.cooperativa.met.TestRedisConfig.class)
class RefreshTokenReuseIntegrationTest {

    @Autowired
    private RefreshTokenUseCase refreshTokenUseCase;
    @Autowired
    private TokenPort tokenPort;
    @Autowired
    private RefreshTokenRepositoryPort refreshTokenRepository;

    @Test
    void shouldKeepAllSessionsClosedAfterDetectingReuseOfAnAlreadyUsedRefreshToken() {
        UUID userId = UUID.randomUUID();
        String stolenToken = tokenPort.generateRefreshToken(userId);
        UUID stolenJti = persist(stolenToken, userId, true);                       // ya fue usado (rotado)
        UUID otherSessionJti = persist(tokenPort.generateRefreshToken(userId), userId, false);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> refreshTokenUseCase.execute(stolenToken));

        assertEquals("REFRESH_TOKEN_REUSED", ex.getCode());
        assertTrue(refreshTokenRepository.findByJti(otherSessionJti).orElseThrow().revoked(),
                "la otra sesión del usuario debe quedar cerrada aunque el caso de uso termine en error");
        assertTrue(refreshTokenRepository.findByJti(stolenJti).orElseThrow().revoked());
    }

    private UUID persist(String refreshToken, UUID userId, boolean revoked) {
        UUID jti = tokenPort.validateRefreshTokenClaims(refreshToken).jti();
        Instant now = Instant.now();
        refreshTokenRepository.save(new RefreshToken(jti, userId, now, now.plus(Duration.ofDays(7)), revoked));
        return jti;
    }
}
