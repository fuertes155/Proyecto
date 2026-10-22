package com.cooperativa.met.application.identity.usecase;

import com.cooperativa.met.application.identity.dto.AuthResponse;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.identity.model.RefreshToken;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.model.UserStatus;
import com.cooperativa.met.domain.identity.port.RefreshTokenRepositoryPort;
import com.cooperativa.met.domain.identity.port.TokenPort;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import com.cooperativa.met.infrastructure.config.MetSecurityProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenUseCaseTest {

    private static final long REFRESH_EXPIRATION_MS = Duration.ofDays(7).toMillis();

    @Mock
    private TokenPort tokenPort;
    @Mock
    private RefreshTokenRepositoryPort refreshTokenRepository;
    @Mock
    private UserRepositoryPort userRepository;

    private RefreshTokenUseCase refreshTokenUseCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID jti = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        MetSecurityProperties securityProperties = new MetSecurityProperties();
        securityProperties.getJwt().setExpirationMs(1_800_000L);
        securityProperties.getJwt().setRefreshExpirationMs(REFRESH_EXPIRATION_MS);
        refreshTokenUseCase = new RefreshTokenUseCase(tokenPort, refreshTokenRepository, userRepository, securityProperties);

        lenient().when(tokenPort.validateRefreshTokenClaims("old-refresh"))
                .thenReturn(new TokenPort.RefreshTokenClaims(userId, jti));
    }

    @Test
    void shouldRotateRefreshTokenAndRevokeTheOldOne() {
        UUID newJti = UUID.randomUUID();
        when(refreshTokenRepository.findByJti(jti)).thenReturn(Optional.of(persisted(false, Instant.now().plusSeconds(3600))));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(UserStatus.ACTIVE)));
        when(tokenPort.generateAccessToken(userId, null)).thenReturn("new-access");
        when(tokenPort.generateRefreshToken(userId)).thenReturn("new-refresh");
        when(tokenPort.validateRefreshTokenClaims("new-refresh")).thenReturn(new TokenPort.RefreshTokenClaims(userId, newJti));

        Instant before = Instant.now();
        AuthResponse response = refreshTokenUseCase.execute("old-refresh");

        assertEquals("new-access", response.accessToken());
        assertEquals("new-refresh", response.refreshToken());
        verify(refreshTokenRepository).revoke(jti);
        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(saved.capture());
        assertEquals(newJti, saved.getValue().jti());
        assertEquals(userId, saved.getValue().userId());
        assertFalse(saved.getValue().revoked());
        assertFalse(saved.getValue().expiresAt().isBefore(before.plusMillis(REFRESH_EXPIRATION_MS)));
    }

    @Test
    void shouldCloseEverySessionWhenAnAlreadyUsedTokenIsPresentedAgain() {
        // Un refresh token ya rotado solo puede volver a aparecer si alguien lo copió.
        when(refreshTokenRepository.findByJti(jti)).thenReturn(Optional.of(persisted(true, Instant.now().plusSeconds(3600))));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> refreshTokenUseCase.execute("old-refresh"));

        assertEquals("REFRESH_TOKEN_REUSED", ex.getCode());
        verify(refreshTokenRepository).revokeAllByUserId(userId);
        verify(tokenPort, never()).generateAccessToken(any(), any());
    }

    @ParameterizedTest
    @EnumSource(value = UserStatus.class, names = {"BLOCKED", "SUSPENDED", "PENDING_VERIFICATION"})
    void shouldNotRenewSessionOfAnAccountThatIsNoLongerActive(UserStatus status) {
        when(refreshTokenRepository.findByJti(jti)).thenReturn(Optional.of(persisted(false, Instant.now().plusSeconds(3600))));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(status)));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> refreshTokenUseCase.execute("old-refresh"));

        assertEquals("USER_NOT_ACTIVE", ex.getCode());
        verify(refreshTokenRepository).revokeAllByUserId(userId);
        verify(tokenPort, never()).generateAccessToken(any(), any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void shouldRejectExpiredTokenWithoutIssuingNewOnes() {
        when(refreshTokenRepository.findByJti(jti)).thenReturn(Optional.of(persisted(false, Instant.now().minusSeconds(1))));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> refreshTokenUseCase.execute("old-refresh"));

        assertEquals("INVALID_TOKEN", ex.getCode());
        verify(refreshTokenRepository, never()).revoke(any());
        verify(tokenPort, never()).generateRefreshToken(any());
    }

    @Test
    void shouldRejectTokenThatWasNeverRegistered() {
        when(refreshTokenRepository.findByJti(jti)).thenReturn(Optional.empty());

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> refreshTokenUseCase.execute("old-refresh"));

        assertEquals("INVALID_TOKEN", ex.getCode());
        verify(tokenPort, never()).generateAccessToken(any(), any());
    }

    @Test
    void shouldRejectTokenWithoutJti() {
        when(tokenPort.validateRefreshTokenClaims("no-jti")).thenReturn(new TokenPort.RefreshTokenClaims(userId, null));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> refreshTokenUseCase.execute("no-jti"));

        assertEquals("INVALID_TOKEN", ex.getCode());
        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    void shouldRejectTokenOfDeletedUser() {
        when(refreshTokenRepository.findByJti(jti)).thenReturn(Optional.of(persisted(false, Instant.now().plusSeconds(3600))));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> refreshTokenUseCase.execute("old-refresh"));

        assertEquals("INVALID_TOKEN", ex.getCode());
        assertTrue(ex.getMessage().contains("Usuario"));
        verify(tokenPort, never()).generateAccessToken(any(), any());
    }

    private RefreshToken persisted(boolean revoked, Instant expiresAt) {
        return new RefreshToken(jti, userId, Instant.now().minusSeconds(60), expiresAt, revoked);
    }

    private User user(UserStatus status) {
        return User.builder().id(userId).status(status).build();
    }
}
