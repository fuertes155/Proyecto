package com.cooperativa.met.infrastructure.security;

import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.identity.port.TokenPort;
import com.cooperativa.met.infrastructure.config.MetSecurityProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para JwtTokenAdapter.
 * No requieren Spring Context — se construye el adapter manualmente con propiedades de test.
 */
class JwtTokenAdapterTest {

    private static final String TEST_SECRET =
            "test-secret-key-that-is-at-least-32-bytes-long-for-hs256-algorithm!";

    private JwtTokenAdapter adapter;

    @BeforeEach
    void setUp() {
        MetSecurityProperties props = new MetSecurityProperties();
        MetSecurityProperties.Jwt jwt = new MetSecurityProperties.Jwt();
        jwt.setSecret(TEST_SECRET);
        jwt.setExpirationMs(1_800_000L);       // 30 minutos
        jwt.setRefreshExpirationMs(604_800_000L); // 7 días
        props.setJwt(jwt);
        props.setEncryption(new MetSecurityProperties.Encryption());

        adapter = new JwtTokenAdapter(props);
        adapter.init(); // @PostConstruct manual
    }

    // ── Access Token ──────────────────────────────────────────────────────────

    @Test
    void generateAccessToken_returnsNonNullToken() {
        UUID userId = UUID.randomUUID();
        String token = adapter.generateAccessToken(userId, "test@met.com");
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void validateAccessToken_returnsCorrectUserId() {
        UUID userId = UUID.randomUUID();
        String token = adapter.generateAccessToken(userId, "test@met.com");

        UUID extracted = adapter.validateAccessToken(token);

        assertEquals(userId, extracted);
    }

    @Test
    void validateAccessToken_throwsBusinessRuleException_forRefreshToken() {
        // Un refresh token no debe validarse como access token
        UUID userId = UUID.randomUUID();
        String refreshToken = adapter.generateRefreshToken(userId);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> adapter.validateAccessToken(refreshToken));

        assertEquals("INVALID_TOKEN", ex.getCode());
    }

    @Test
    void validateAccessToken_throwsBusinessRuleException_forTamperedToken() {
        UUID userId = UUID.randomUUID();
        String token = adapter.generateAccessToken(userId, "test@met.com");
        String tampered = token.substring(0, token.length() - 4) + "XXXX";

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> adapter.validateAccessToken(tampered));

        assertEquals("INVALID_TOKEN", ex.getCode());
    }

    @Test
    void validateAccessToken_throwsBusinessRuleException_forEmptyToken() {
        assertThrows(BusinessRuleException.class,
                () -> adapter.validateAccessToken(""));
    }

    // ── Refresh Token ─────────────────────────────────────────────────────────

    @Test
    void generateRefreshToken_returnsNonNullToken() {
        String token = adapter.generateRefreshToken(UUID.randomUUID());
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void validateRefreshTokenClaims_returnsCorrectUserIdAndJti() {
        UUID userId = UUID.randomUUID();
        String token = adapter.generateRefreshToken(userId);

        TokenPort.RefreshTokenClaims claims = adapter.validateRefreshTokenClaims(token);

        assertEquals(userId, claims.userId());
        assertNotNull(claims.jti()); // El refresh token lleva JTI para revocación
    }

    @Test
    void validateRefreshTokenClaims_throwsBusinessRuleException_forAccessToken() {
        UUID userId = UUID.randomUUID();
        String accessToken = adapter.generateAccessToken(userId, "x@met.com");

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> adapter.validateRefreshTokenClaims(accessToken));

        assertEquals("INVALID_TOKEN", ex.getCode());
    }

    // ── Admin Token ───────────────────────────────────────────────────────────

    @Test
    void generateAdminAccessToken_roleExtractedCorrectly() {
        UUID adminId = UUID.randomUUID();
        String token = adapter.generateAdminAccessToken(adminId, "admin_user", "SUPERADMIN");

        String role = adapter.extractRole(token);

        assertEquals("SUPERADMIN", role);
    }

    @Test
    void extractRole_returnsNull_forTokenWithoutRole() {
        // Un access token de usuario normal no tiene claim "role"
        String token = adapter.generateAccessToken(UUID.randomUUID(), "user@met.com");

        String role = adapter.extractRole(token);

        assertNull(role);
    }

    // ── Diferente secret key ──────────────────────────────────────────────────

    @Test
    void validateAccessToken_throwsBusinessRuleException_whenSignedWithDifferentSecret() {
        // Crear un adapter con diferente clave
        MetSecurityProperties otherProps = new MetSecurityProperties();
        MetSecurityProperties.Jwt otherJwt = new MetSecurityProperties.Jwt();
        otherJwt.setSecret("different-secret-key-that-is-also-at-least-32-bytes-long!!!");
        otherJwt.setExpirationMs(1_800_000L);
        otherJwt.setRefreshExpirationMs(604_800_000L);
        otherProps.setJwt(otherJwt);
        otherProps.setEncryption(new MetSecurityProperties.Encryption());

        JwtTokenAdapter otherAdapter = new JwtTokenAdapter(otherProps);
        otherAdapter.init();

        String tokenFromOtherAdapter = otherAdapter.generateAccessToken(UUID.randomUUID(), "x@met.com");

        // Validar con el adapter original (distinta clave) debe fallar
        assertThrows(BusinessRuleException.class,
                () -> adapter.validateAccessToken(tokenFromOtherAdapter));
    }
}
