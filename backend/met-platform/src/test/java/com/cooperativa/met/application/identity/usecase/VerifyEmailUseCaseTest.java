package com.cooperativa.met.application.identity.usecase;

import com.cooperativa.met.application.identity.dto.VerifyEmailRequest;
import com.cooperativa.met.application.identity.service.OtpService;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.common.exception.ResourceNotFoundException;
import com.cooperativa.met.domain.identity.model.DocumentType;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.model.UserStatus;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
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
class VerifyEmailUseCaseTest {

    private static final String DOCUMENT = "1086578123";
    private static final VerifyEmailRequest REQUEST = new VerifyEmailRequest(DocumentType.CC, DOCUMENT, "482913");

    @Mock
    private UserRepositoryPort userRepository;
    @Mock
    private OtpService otpService;

    @InjectMocks
    private VerifyEmailUseCase verifyEmailUseCase;

    @Test
    void shouldActivateAccountWhenCodeIsValid() {
        givenUser(UserStatus.PENDING_VERIFICATION);
        when(otpService.validateEmailVerificationOtp(DOCUMENT, "482913")).thenReturn(true);

        verifyEmailUseCase.execute(REQUEST);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertEquals(UserStatus.ACTIVE, saved.getValue().getStatus());
    }

    @Test
    void shouldKeepAccountPendingWhenCodeIsWrong() {
        givenUser(UserStatus.PENDING_VERIFICATION);
        when(otpService.validateEmailVerificationOtp(DOCUMENT, "482913")).thenReturn(false);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> verifyEmailUseCase.execute(REQUEST));

        assertEquals("INVALID_OTP", ex.getCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldNotReactivateBlockedAccountThroughEmailVerification() {
        // Un usuario bloqueado no puede volver a ACTIVE "verificando el correo" otra vez.
        givenUser(UserStatus.BLOCKED);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> verifyEmailUseCase.execute(REQUEST));

        assertEquals("ALREADY_VERIFIED", ex.getCode());
        verifyNoInteractions(otpService);
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldFailForUnknownDocument() {
        when(userRepository.findByDocument(DocumentType.CC, DOCUMENT)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> verifyEmailUseCase.execute(REQUEST));

        verifyNoInteractions(otpService);
    }

    private void givenUser(UserStatus status) {
        when(userRepository.findByDocument(DocumentType.CC, DOCUMENT))
                .thenReturn(Optional.of(User.builder().id(UUID.randomUUID()).documentNumber(DOCUMENT).status(status).build()));
    }
}
