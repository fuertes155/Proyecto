package com.cooperativa.met.application.identity.usecase;

import com.cooperativa.met.application.identity.dto.EnableBiometricLoginRequest;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.identity.model.DocumentType;
import com.cooperativa.met.domain.identity.model.KycStatus;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.model.UserStatus;
import com.cooperativa.met.domain.identity.port.EncryptionPort;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import com.cooperativa.met.infrastructure.audit.AuditLogService;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManageBiometricLoginUseCaseTest {

    private static final String TOKEN = "k3Yd8Q2mVt0pX7wZr4nB6cJ1sH9fL5aE";

    @Mock
    private UserRepositoryPort userRepository;
    @Mock
    private EncryptionPort encryptionPort;
    @Mock
    private PinAttemptService pinAttemptService;
    @Mock
    private AuditLogService auditLogService;

    private ManageBiometricLoginUseCase useCase;
    private User user;

    @BeforeEach
    void setUp() {
        useCase = new ManageBiometricLoginUseCase(userRepository, encryptionPort, pinAttemptService, auditLogService);
        user = User.builder()
                .id(UUID.randomUUID())
                .documentType(DocumentType.CC)
                .documentNumber("1086578123")
                .firstName("Juan")
                .lastName("Pabón Gómez")
                .pinHash("pin-hash")
                .status(UserStatus.ACTIVE)
                .kycStatus(KycStatus.APPROVED)
                .build();
    }

    private void givenUser(User existing) {
        when(userRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
    }

    @Test
    void shouldStoreOnlyTheHashOfTheDeviceTokenWhenThePinIsCorrect() {
        givenUser(user);
        when(encryptionPort.decryptRsa("encrypted-pin")).thenReturn("1234");
        when(encryptionPort.verifyPin("1234", "pin-hash")).thenReturn(true);
        when(encryptionPort.hashBiometric(TOKEN)).thenReturn("token-hash");

        useCase.enable(user.getId(), new EnableBiometricLoginRequest("encrypted-pin", TOKEN));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertEquals("token-hash", saved.getValue().getBiometricHash());
        verify(pinAttemptService).resetAttempts(user.getId());
    }

    @Test
    void shouldNotEnableAndShouldCountTheAttemptWhenThePinIsWrong() {
        givenUser(user);
        when(encryptionPort.decryptRsa("encrypted-pin")).thenReturn("9999");
        when(encryptionPort.verifyPin("9999", "pin-hash")).thenReturn(false);
        doThrow(new BusinessRuleException("INVALID_PIN", "El PIN ingresado es incorrecto. Intento 1 de 5"))
                .when(pinAttemptService).checkAndRecordFailure(user.getId());

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> useCase.enable(user.getId(), new EnableBiometricLoginRequest("encrypted-pin", TOKEN)));

        assertEquals("INVALID_PIN", ex.getCode());
        verify(userRepository, never()).save(any());
        verify(encryptionPort, never()).hashBiometric(any());
    }

    @Test
    void shouldNotCheckThePinWhileTheAccountIsLocked() {
        givenUser(user);
        doThrow(new BusinessRuleException("ACCOUNT_LOCKED", "Tu cuenta está bloqueada temporalmente."))
                .when(pinAttemptService).checkBlocked(user.getId());

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> useCase.enable(user.getId(), new EnableBiometricLoginRequest("encrypted-pin", TOKEN)));

        assertEquals("ACCOUNT_LOCKED", ex.getCode());
        verify(encryptionPort, never()).verifyPin(any(), any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldRemoveTheStoredHashWhenDisabled() {
        givenUser(user.withBiometricHash("token-hash"));

        useCase.disable(user.getId());

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertNull(saved.getValue().getBiometricHash());
    }
}
