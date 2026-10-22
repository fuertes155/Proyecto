package com.cooperativa.met.application.identity.usecase;

import com.cooperativa.met.application.identity.dto.PinRecoveryResetRequest;
import com.cooperativa.met.application.identity.service.OtpService;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.common.exception.ResourceNotFoundException;
import com.cooperativa.met.domain.identity.model.DocumentType;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.port.EncryptionPort;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResetPinWithOtpUseCaseTest {

    private static final String DOCUMENT = "1086578123";

    @Mock
    private UserRepositoryPort userRepository;
    @Mock
    private OtpService otpService;
    @Mock
    private EncryptionPort encryptionPort;

    @InjectMocks
    private ResetPinWithOtpUseCase resetPinWithOtpUseCase;

    private PinRecoveryResetRequest request;

    @BeforeEach
    void setUp() {
        request = new PinRecoveryResetRequest();
        request.setDocumentType(DocumentType.CC);
        request.setDocumentNumber(DOCUMENT);
        request.setOtpCode("482913");
        request.setNewPin("rsa(5678)");
    }

    @Test
    void shouldSetNewPinWhenOtpIsValid() {
        givenUser();
        when(otpService.validateOtp(DOCUMENT, "482913")).thenReturn(true);
        when(encryptionPort.decryptRsa("rsa(5678)")).thenReturn("5678");
        when(encryptionPort.hashPin("5678")).thenReturn("new-hash");

        resetPinWithOtpUseCase.execute(request);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertEquals("new-hash", saved.getValue().getPinHash());
    }

    @Test
    void shouldNotTouchPinWhenOtpIsWrongOrExpired() {
        givenUser();
        when(otpService.validateOtp(DOCUMENT, "482913")).thenReturn(false);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> resetPinWithOtpUseCase.execute(request));

        assertEquals("INVALID_OTP", ex.getCode());
        verifyNoInteractions(encryptionPort);
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldRejectPinThatCannotBeDecrypted() {
        givenUser();
        when(otpService.validateOtp(DOCUMENT, "482913")).thenReturn(true);
        when(encryptionPort.decryptRsa("rsa(5678)")).thenThrow(new IllegalStateException("bad padding"));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> resetPinWithOtpUseCase.execute(request));

        assertEquals("SECURITY_ERROR", ex.getCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldFailForUnknownDocumentWithoutTouchingOtps() {
        when(userRepository.findByDocument(DocumentType.CC, DOCUMENT)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> resetPinWithOtpUseCase.execute(request));

        verifyNoInteractions(otpService, encryptionPort);
    }

    private void givenUser() {
        when(userRepository.findByDocument(DocumentType.CC, DOCUMENT))
                .thenReturn(Optional.of(User.builder().id(UUID.randomUUID()).documentNumber(DOCUMENT).pinHash("old-hash").build()));
    }
}
