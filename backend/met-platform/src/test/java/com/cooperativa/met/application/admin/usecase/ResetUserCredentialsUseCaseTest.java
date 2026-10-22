package com.cooperativa.met.application.admin.usecase;

import com.cooperativa.met.application.admin.dto.ResetCredentialsRequest;
import com.cooperativa.met.domain.admin.model.AdminAuditEntry;
import com.cooperativa.met.domain.admin.port.AdminAuditLogPort;
import com.cooperativa.met.domain.common.exception.ResourceNotFoundException;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.model.UserStatus;
import com.cooperativa.met.domain.identity.port.RefreshTokenRepositoryPort;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResetUserCredentialsUseCaseTest {

    @Mock
    private UserRepositoryPort userRepository;
    @Mock
    private RefreshTokenRepositoryPort refreshTokenRepository;
    @Mock
    private AdminAuditLogPort auditLog;

    @InjectMocks
    private ResetUserCredentialsUseCase resetUserCredentialsUseCase;

    private final UUID adminId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @Test
    void shouldWipePinAndBiometricsAndCloseSessions() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(User.builder()
                .id(userId)
                .documentNumber("1086578123")
                .status(UserStatus.ACTIVE)
                .pinHash("$argon2id$real-pin-hash")
                .biometricHash("real-biometric-hash")
                .build()));

        resetUserCredentialsUseCase.execute(adminId, userId, new ResetCredentialsRequest("Socio perdió el celular"), "181.50.10.2");

        verify(refreshTokenRepository).revokeAllByUserId(userId);
        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertNull(saved.getValue().getPinHash());
        assertNull(saved.getValue().getBiometricHash());
        assertEquals("1086578123", saved.getValue().getDocumentNumber());
        assertEquals(UserStatus.ACTIVE, saved.getValue().getStatus());
    }

    @Test
    void shouldNeverWriteTheRealHashesToTheAuditLog() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(User.builder()
                .id(userId).pinHash("$argon2id$real-pin-hash").biometricHash("real-biometric-hash").build()));

        resetUserCredentialsUseCase.execute(adminId, userId, new ResetCredentialsRequest("motivo"), "181.50.10.2");

        ArgumentCaptor<AdminAuditEntry> entry = ArgumentCaptor.forClass(AdminAuditEntry.class);
        verify(auditLog).log(entry.capture());
        assertEquals("USER_CREDENTIALS_RESET", entry.getValue().getAccion());
        String audited = entry.getValue().getValoresAnteriores() + entry.getValue().getValoresNuevos();
        assertFalse(audited.contains("real-pin-hash"), audited);
        assertFalse(audited.contains("real-biometric-hash"), audited);
    }

    @Test
    void shouldFailForUnknownUser() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                resetUserCredentialsUseCase.execute(adminId, userId, new ResetCredentialsRequest("motivo"), "ip"));

        verifyNoInteractions(refreshTokenRepository, auditLog);
    }
}
