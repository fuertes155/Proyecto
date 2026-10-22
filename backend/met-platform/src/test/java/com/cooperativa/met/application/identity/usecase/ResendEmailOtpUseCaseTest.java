package com.cooperativa.met.application.identity.usecase;

import com.cooperativa.met.application.identity.dto.ResendEmailOtpRequest;
import com.cooperativa.met.application.identity.service.OtpService;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.identity.model.DocumentType;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.model.UserStatus;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResendEmailOtpUseCaseTest {

    private static final String DOCUMENT = "1086578123";
    private static final ResendEmailOtpRequest REQUEST = new ResendEmailOtpRequest(DocumentType.CC, DOCUMENT);

    @Mock
    private UserRepositoryPort userRepository;
    @Mock
    private OtpService otpService;

    @InjectMocks
    private ResendEmailOtpUseCase resendEmailOtpUseCase;

    @Test
    void shouldSendNewCodeToTheEmailOnFile() {
        givenUser(UserStatus.PENDING_VERIFICATION);

        resendEmailOtpUseCase.execute(REQUEST);

        verify(otpService).generateAndSendEmailVerificationOtp(DOCUMENT, "socio@example.com");
    }

    @Test
    void shouldRefuseWhenAccountIsAlreadyVerified() {
        givenUser(UserStatus.ACTIVE);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> resendEmailOtpUseCase.execute(REQUEST));

        assertEquals("ALREADY_VERIFIED", ex.getCode());
        verifyNoInteractions(otpService);
    }

    @Test
    void shouldFailForUnknownDocument() {
        when(userRepository.findByDocument(DocumentType.CC, DOCUMENT)).thenReturn(Optional.empty());

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> resendEmailOtpUseCase.execute(REQUEST));

        assertEquals("USER_NOT_FOUND", ex.getCode());
        verifyNoInteractions(otpService);
    }

    private void givenUser(UserStatus status) {
        when(userRepository.findByDocument(DocumentType.CC, DOCUMENT)).thenReturn(Optional.of(User.builder()
                .id(UUID.randomUUID())
                .documentNumber(DOCUMENT)
                .email("socio@example.com")
                .status(status)
                .build()));
    }
}
