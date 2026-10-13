package com.cooperativa.met.application.identity.usecase;

import com.cooperativa.met.application.identity.dto.UpdateProfileRequest;
import com.cooperativa.met.application.identity.dto.UserResponse;
import com.cooperativa.met.application.identity.mapper.UserMapper;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.identity.model.DocumentType;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UpdateProfileUseCaseTest {

    private static final String EMAIL = "socio@example.com";

    @Mock
    private UserRepositoryPort userRepository;

    private UpdateProfileUseCase updateProfileUseCase;

    private UUID userId;
    private UpdateProfileRequest request;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        updateProfileUseCase = new UpdateProfileUseCase(userRepository, new UserMapper());

        userId = UUID.randomUUID();
        request = new UpdateProfileRequest();
        request.setEmail(EMAIL);
        request.setPhone("3001234567");

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldAddContactDataToAccountRegisteredWithoutEmail() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId, null)));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        UserResponse response = updateProfileUseCase.execute(userId, request);

        assertEquals(EMAIL, response.email());
        assertEquals("3001234567", response.phone());
    }

    @Test
    void shouldRejectEmailRegisteredByAnotherUser() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId, null)));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user(UUID.randomUUID(), EMAIL)));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> updateProfileUseCase.execute(userId, request));

        assertEquals("EMAIL_ALREADY_EXISTS", ex.getCode());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldAllowUserToKeepOwnEmail() {
        User current = user(userId, EMAIL);
        when(userRepository.findById(userId)).thenReturn(Optional.of(current));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(current));

        UserResponse response = updateProfileUseCase.execute(userId, request);

        assertEquals(EMAIL, response.email());
    }

    private User user(UUID id, String email) {
        return User.builder()
                .id(id)
                .documentType(DocumentType.CC)
                .documentNumber("1086578123")
                .email(email)
                .firstName("Juan")
                .lastName("Pabón Gómez")
                .build();
    }
}
