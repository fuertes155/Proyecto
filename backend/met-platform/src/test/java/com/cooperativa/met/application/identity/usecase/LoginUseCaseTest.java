package com.cooperativa.met.application.identity.usecase;

import com.cooperativa.met.application.identity.dto.AuthResponse;
import com.cooperativa.met.application.identity.dto.LoginRequest;
import com.cooperativa.met.application.identity.service.OtpService;
import com.cooperativa.met.application.security.FraudDetectionService;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.identity.model.DocumentType;
import com.cooperativa.met.domain.identity.model.KycStatus;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.model.UserStatus;
import com.cooperativa.met.domain.identity.port.EncryptionPort;
import com.cooperativa.met.domain.identity.port.NotificationPort;
import com.cooperativa.met.domain.identity.port.RefreshTokenRepositoryPort;
import com.cooperativa.met.domain.identity.port.TokenPort;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import com.cooperativa.met.infrastructure.config.MetSecurityProperties;
import com.cooperativa.met.infrastructure.security.PinAttemptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginUseCaseTest {

    private static final String DOCUMENT = "1086578123";

    @Mock
    private UserRepositoryPort userRepository;
    @Mock
    private EncryptionPort encryptionPort;
    @Mock
    private TokenPort tokenPort;
    @Mock
    private RefreshTokenRepositoryPort refreshTokenRepository;
    @Mock
    private NotificationPort notificationPort;
    @Mock
    private OtpService otpService;
    @Mock
    private FraudDetectionService fraudDetectionService;
    @Mock
    private PinAttemptService pinAttemptService;

    private LoginUseCase loginUseCase;

    @BeforeEach
    void setUp() {
        MetSecurityProperties securityProperties = new MetSecurityProperties();
        securityProperties.getJwt().setExpirationMs(3_600_000L);
        securityProperties.getJwt().setRefreshExpirationMs(604_800_000L);

        loginUseCase = new LoginUseCase(userRepository, encryptionPort, tokenPort, refreshTokenRepository,
                securityProperties, notificationPort, otpService, fraudDetectionService, pinAttemptService);
    }

    @Test
    void shouldBindNewDeviceWithoutOtpWhenUserHasNoEmail() {
        // Cuenta creada con el registro simplificado (sin correo): no hay a dónde enviar el
        // código de dispositivo nuevo ni la alerta de IP nueva.
        User user = activeUser().lastKnownDeviceId("old-device").lastKnownIp("190.0.0.1").build();
        when(userRepository.findByDocument(DocumentType.CC, DOCUMENT)).thenReturn(Optional.of(user));
        stubPinVerification(true);
        when(tokenPort.generateAccessToken(user.getId(), null)).thenReturn("access-token");
        when(tokenPort.generateRefreshToken(user.getId())).thenReturn("refresh-token");
        when(tokenPort.validateRefreshTokenClaims("refresh-token"))
                .thenReturn(new TokenPort.RefreshTokenClaims(user.getId(), UUID.randomUUID()));

        AuthResponse response = loginUseCase.execute(loginRequest("new-device"), "190.0.0.2");

        assertEquals("access-token", response.accessToken());
        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertEquals("new-device", saved.getValue().getLastKnownDeviceId());
        verify(pinAttemptService).resetAttempts(user.getId());
        verifyNoInteractions(otpService, notificationPort);
    }

    @Test
    void shouldStillRequireDeviceOtpWhenUserHasEmail() {
        User user = activeUser().email("socio@example.com").lastKnownDeviceId("old-device").build();
        when(userRepository.findByDocument(DocumentType.CC, DOCUMENT)).thenReturn(Optional.of(user));
        stubPinVerification(true);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> loginUseCase.execute(loginRequest("new-device"), "190.0.0.1"));

        assertEquals("DEVICE_NOT_RECOGNIZED", ex.getCode());
        verify(otpService).generateAndSendOtp(DOCUMENT, "socio@example.com");
        verify(tokenPort, never()).generateAccessToken(any(), any());
    }

    @Test
    void shouldCountWrongPinTowardsTemporaryLock() {
        User user = activeUser().build();
        when(userRepository.findByDocument(DocumentType.CC, DOCUMENT)).thenReturn(Optional.of(user));
        stubPinVerification(false);
        doThrow(new BusinessRuleException("ACCOUNT_LOCKED", "Tu cuenta ha sido bloqueada por 15 minutos."))
                .when(pinAttemptService).checkAndRecordFailure(user.getId());

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> loginUseCase.execute(loginRequest("device-1"), "190.0.0.1"));

        assertEquals("ACCOUNT_LOCKED", ex.getCode());
        verify(pinAttemptService, never()).resetAttempts(any());
        verifyNoInteractions(tokenPort);
    }

    @Test
    void shouldRejectLoginWithoutCheckingPinWhileLocked() {
        User user = activeUser().build();
        when(userRepository.findByDocument(DocumentType.CC, DOCUMENT)).thenReturn(Optional.of(user));
        doThrow(new BusinessRuleException("ACCOUNT_LOCKED", "Tu cuenta está bloqueada temporalmente."))
                .when(pinAttemptService).checkBlocked(user.getId());

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> loginUseCase.execute(loginRequest("device-1"), "190.0.0.1"));

        assertEquals("ACCOUNT_LOCKED", ex.getCode());
        verifyNoInteractions(encryptionPort, tokenPort);
    }

    @Test
    void shouldNeverLoginWithWrongPinEvenIfAttemptServiceDoesNotThrow() {
        User user = activeUser().build();
        when(userRepository.findByDocument(DocumentType.CC, DOCUMENT)).thenReturn(Optional.of(user));
        stubPinVerification(false);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> loginUseCase.execute(loginRequest("device-1"), "190.0.0.1"));

        assertEquals("INVALID_CREDENTIALS", ex.getCode());
        verifyNoInteractions(tokenPort);
    }

    private void stubPinVerification(boolean valid) {
        when(encryptionPort.decryptRsa("encrypted-pin")).thenReturn("1234");
        when(encryptionPort.verifyPin("1234", "pin-hash")).thenReturn(valid);
    }

    private LoginRequest loginRequest(String deviceId) {
        return new LoginRequest(DocumentType.CC, DOCUMENT, deviceId, "encrypted-pin", null, null);
    }

    private User.UserBuilder activeUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .documentType(DocumentType.CC)
                .documentNumber(DOCUMENT)
                .firstName("Juan")
                .lastName("Pabón Gómez")
                .pinHash("pin-hash")
                .status(UserStatus.ACTIVE)
                .kycStatus(KycStatus.PENDING);
    }
}
