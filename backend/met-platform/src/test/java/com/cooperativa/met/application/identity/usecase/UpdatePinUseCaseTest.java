package com.cooperativa.met.application.identity.usecase;

import com.cooperativa.met.application.identity.dto.UpdatePinRequest;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.common.exception.ResourceNotFoundException;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.port.EncryptionPort;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import com.cooperativa.met.infrastructure.audit.AuditLogService;
import com.cooperativa.met.infrastructure.security.PinAttemptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdatePinUseCaseTest {

    @Mock
    private UserRepositoryPort userRepository;
    @Mock
    private EncryptionPort encryptionPort;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private PinAttemptService pinAttemptService;

    @InjectMocks
    private UpdatePinUseCase updatePinUseCase;

    private final UUID userId = UUID.randomUUID();
    private UpdatePinRequest request;

    @BeforeEach
    void setUp() {
        request = new UpdatePinRequest();
        request.setCurrentPin("rsa(1234)");
        request.setNewPin("rsa(5678)");
    }

    @Test
    void shouldReplacePinHashWhenCurrentPinIsCorrect() {
        givenUser();
        givenDecryptedPins();
        when(encryptionPort.verifyPin("1234", "old-hash")).thenReturn(true);
        when(encryptionPort.hashPin("5678")).thenReturn("new-hash");

        updatePinUseCase.execute(userId, request);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertEquals("new-hash", saved.getValue().getPinHash());
        verify(pinAttemptService).resetAttempts(userId);
        verify(auditLogService).logSuccess(userId, AuditLogService.PIN_CHANGED, "USER", userId.toString(), null);
    }

    @Test
    void shouldCountFailedAttemptAndKeepOldPinWhenCurrentPinIsWrong() {
        givenUser();
        givenDecryptedPins();
        when(encryptionPort.verifyPin("1234", "old-hash")).thenReturn(false);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> updatePinUseCase.execute(userId, request));

        assertEquals("INVALID_PIN", ex.getCode());
        verify(pinAttemptService).checkAndRecordFailure(userId);
        verify(userRepository, never()).save(any());
        verify(auditLogService).logFailure(eq(userId), eq(AuditLogService.PIN_CHANGED), eq("USER"),
                eq(userId.toString()), contains("INVALID_CURRENT_PIN"));
    }

    @Test
    void shouldNotEvenCheckThePinWhileUserIsBlockedForFailedAttempts() {
        givenUser();
        doThrow(new BusinessRuleException("ACCOUNT_LOCKED", "Bloqueado"))
                .when(pinAttemptService).checkBlocked(userId);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> updatePinUseCase.execute(userId, request));

        assertEquals("ACCOUNT_LOCKED", ex.getCode());
        verifyNoInteractions(encryptionPort);
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldRejectPinThatCannotBeDecryptedWithoutCountingItAsWrongPin() {
        givenUser();
        when(encryptionPort.decryptRsa(anyString())).thenThrow(new IllegalStateException("bad padding"));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> updatePinUseCase.execute(userId, request));

        assertEquals("SECURITY_ERROR", ex.getCode());
        verify(pinAttemptService, never()).checkAndRecordFailure(any());
        verify(userRepository, never()).save(any());
        verify(auditLogService).logFailure(eq(userId), eq(AuditLogService.PIN_CHANGED), eq("USER"),
                eq(userId.toString()), contains("SECURITY_ERROR_RSA"));
    }

    @Test
    void shouldFailForUnknownUser() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> updatePinUseCase.execute(userId, request));

        verifyNoInteractions(encryptionPort, pinAttemptService);
    }

    private void givenUser() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(User.builder().id(userId).pinHash("old-hash").build()));
    }

    private void givenDecryptedPins() {
        when(encryptionPort.decryptRsa("rsa(1234)")).thenReturn("1234");
        when(encryptionPort.decryptRsa("rsa(5678)")).thenReturn("5678");
    }
}
