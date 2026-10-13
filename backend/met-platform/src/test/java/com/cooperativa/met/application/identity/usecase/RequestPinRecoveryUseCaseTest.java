package com.cooperativa.met.application.identity.usecase;

import com.cooperativa.met.application.identity.dto.PinRecoveryRequest;
import com.cooperativa.met.application.identity.service.OtpService;
import com.cooperativa.met.domain.identity.model.DocumentType;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RequestPinRecoveryUseCaseTest {

    private static final String DOCUMENT = "1086578123";

    @Mock
    private UserRepositoryPort userRepository;
    @Mock
    private OtpService otpService;

    @InjectMocks
    private RequestPinRecoveryUseCase requestPinRecoveryUseCase;

    private PinRecoveryRequest request;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        request = new PinRecoveryRequest();
        request.setDocumentType(DocumentType.CC);
        request.setDocumentNumber(DOCUMENT);
    }

    @Test
    void shouldSendOtpToRegisteredEmail() {
        when(userRepository.findByDocument(DocumentType.CC, DOCUMENT))
                .thenReturn(Optional.of(user("socio@example.com")));

        requestPinRecoveryUseCase.execute(request);

        verify(otpService).generateAndSendOtp(DOCUMENT, "socio@example.com");
    }

    @Test
    void shouldAnswerLikeUnknownDocumentWhenAccountHasNoEmail() {
        when(userRepository.findByDocument(DocumentType.CC, DOCUMENT)).thenReturn(Optional.of(user(null)));

        assertDoesNotThrow(() -> requestPinRecoveryUseCase.execute(request));

        verifyNoInteractions(otpService);
    }

    private User user(String email) {
        return User.builder()
                .id(UUID.randomUUID())
                .documentType(DocumentType.CC)
                .documentNumber(DOCUMENT)
                .email(email)
                .build();
    }
}
