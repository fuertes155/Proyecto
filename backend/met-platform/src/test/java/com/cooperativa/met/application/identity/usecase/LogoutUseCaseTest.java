package com.cooperativa.met.application.identity.usecase;

import com.cooperativa.met.domain.identity.port.RefreshTokenRepositoryPort;
import com.cooperativa.met.infrastructure.config.MetSecurityProperties;
import com.cooperativa.met.infrastructure.security.TokenBlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LogoutUseCaseTest {

    @Mock
    private TokenBlacklistService tokenBlacklist;
    @Mock
    private RefreshTokenRepositoryPort refreshTokenRepository;

    private LogoutUseCase logoutUseCase;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        MetSecurityProperties securityProperties = new MetSecurityProperties();
        securityProperties.getJwt().setExpirationMs(1_800_000L);
        logoutUseCase = new LogoutUseCase(tokenBlacklist, refreshTokenRepository, securityProperties);
    }

    @Test
    void shouldBlacklistAccessTokenForItsWholeLifetimeAndRevokeRefreshTokens() {
        logoutUseCase.execute(userId, "access-token");

        verify(tokenBlacklist).revoke("access-token", Duration.ofMinutes(30));
        verify(refreshTokenRepository).revokeAllByUserId(userId);
    }

    @Test
    void shouldStillBlacklistAccessTokenWhenRevokingRefreshTokensFails() {
        doThrow(new IllegalStateException("BD caída")).when(refreshTokenRepository).revokeAllByUserId(userId);

        assertDoesNotThrow(() -> logoutUseCase.execute(userId, "access-token"));

        verify(tokenBlacklist).revoke("access-token", Duration.ofMinutes(30));
    }
}
