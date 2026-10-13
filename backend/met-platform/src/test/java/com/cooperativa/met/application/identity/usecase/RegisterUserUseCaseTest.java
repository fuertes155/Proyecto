package com.cooperativa.met.application.identity.usecase;

import com.cooperativa.met.application.identity.dto.RegisterUserRequest;
import com.cooperativa.met.application.identity.dto.UserResponse;
import com.cooperativa.met.application.identity.mapper.UserMapper;
import com.cooperativa.met.application.identity.service.OtpService;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.identity.model.DocumentType;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.model.UserStatus;
import com.cooperativa.met.domain.identity.port.EncryptionPort;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RegisterUserUseCaseTest {

    private static final String DOCUMENT = "1086578123";

    @Mock
    private UserRepositoryPort userRepository;
    @Mock
    private EncryptionPort encryptionPort;
    @Mock
    private OtpService otpService;

    private RegisterUserUseCase registerUserUseCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        registerUserUseCase = new RegisterUserUseCase(userRepository, encryptionPort, new UserMapper(), otpService);

        when(encryptionPort.decryptRsa("encrypted-pin")).thenReturn("1234");
        when(encryptionPort.hashPin("1234")).thenReturn("pin-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldRegisterWithOnlyDocumentFullNameAndPin() {
        RegisterUserRequest request = new RegisterUserRequest(
                DocumentType.CC, DOCUMENT, null, null, null, null, "Juan Carlos Pabón Gómez", "encrypted-pin", true);

        UserResponse response = registerUserUseCase.execute(request);

        assertEquals("Juan Carlos", response.firstName());
        assertEquals("Pabón Gómez", response.lastName());
        assertNull(response.email());
        // Sin correo no hay verificación pendiente: el usuario puede iniciar sesión de inmediato.
        assertEquals(UserStatus.ACTIVE, response.status());
        verify(userRepository, never()).existsByEmail(anyString());
        verifyNoInteractions(otpService);
    }

    @Test
    void shouldKeepEmailVerificationWhenEmailIsProvided() {
        RegisterUserRequest request = new RegisterUserRequest(
                DocumentType.CC, DOCUMENT, "juan@example.com", "3001234567", "Juan", "García", null, "encrypted-pin", true);

        UserResponse response = registerUserUseCase.execute(request);

        assertEquals("Juan", response.firstName());
        assertEquals("García", response.lastName());
        assertEquals(UserStatus.PENDING_VERIFICATION, response.status());
        verify(otpService).generateAndSendEmailVerificationOtp(DOCUMENT, "juan@example.com");
    }

    @Test
    void shouldRejectFullNameWithoutSurname() {
        RegisterUserRequest request = new RegisterUserRequest(
                DocumentType.CC, DOCUMENT, null, null, null, null, "Juan", "encrypted-pin", true);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> registerUserUseCase.execute(request));

        assertEquals("INVALID_FULL_NAME", ex.getCode());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldRejectDocumentAlreadyRegistered() {
        when(userRepository.existsByDocument(DocumentType.CC, DOCUMENT)).thenReturn(true);
        RegisterUserRequest request = new RegisterUserRequest(
                DocumentType.CC, DOCUMENT, null, null, null, null, "Juan Pabón", "encrypted-pin", true);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> registerUserUseCase.execute(request));

        assertEquals("USER_ALREADY_EXISTS", ex.getCode());
        verify(userRepository, never()).save(any(User.class));
    }
}
