package com.cooperativa.met.application.admin.usecase;

import com.cooperativa.met.application.admin.dto.EmergencyLockRequest;
import com.cooperativa.met.domain.admin.model.AdminAuditEntry;
import com.cooperativa.met.domain.admin.port.AdminAuditLogPort;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.common.exception.ResourceNotFoundException;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.model.UserStatus;
import com.cooperativa.met.domain.identity.port.RefreshTokenRepositoryPort;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmergencyLockUseCaseTest {

    private static final String IP = "181.50.10.2";

    @Mock
    private UserRepositoryPort userRepository;
    @Mock
    private RefreshTokenRepositoryPort refreshTokenRepository;
    @Mock
    private AdminAuditLogPort auditLog;

    private EmergencyLockUseCase emergencyLockUseCase;

    private final UUID adminId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        emergencyLockUseCase = new EmergencyLockUseCase(userRepository, refreshTokenRepository, auditLog, new ObjectMapper());
    }

    @Test
    void shouldSuspendUserCloseTheirSessionsAndAuditBeforeAndAfter() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(User.builder().id(userId).status(UserStatus.ACTIVE).build()));

        emergencyLockUseCase.execute(adminId, new EmergencyLockRequest(userId.toString(), "user", "Celular robado"), IP);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertEquals(UserStatus.SUSPENDED, saved.getValue().getStatus());
        verify(refreshTokenRepository).revokeAllByUserId(userId);

        AdminAuditEntry entry = auditEntry();
        assertEquals("EMERGENCY_LOCK_USER", entry.getAccion());
        assertEquals("{\"status\":\"ACTIVE\"}", entry.getValoresAnteriores());
        assertEquals("{\"status\":\"SUSPENDED\"}", entry.getValoresNuevos());
        assertEquals("Celular robado", entry.getMotivo());
        assertEquals(adminId, entry.getActorAdminId());
    }

    @Test
    void shouldOnlyRevokeSessionsForAccessTokenScope() {
        emergencyLockUseCase.execute(adminId, new EmergencyLockRequest(userId.toString(), "ACCESS_TOKEN", "Sesión sospechosa"), IP);

        verify(refreshTokenRepository).revokeAllByUserId(userId);
        verify(userRepository, never()).save(any());
        assertEquals("EMERGENCY_REVOKE_TOKENS", auditEntry().getAccion());
    }

    @Test
    void shouldRejectUnsupportedScope() {
        // El DTO documenta CARD, pero no está implementado: debe fallar explícitamente, no "hacer nada".
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                emergencyLockUseCase.execute(adminId, new EmergencyLockRequest(userId.toString(), "CARD", "x"), IP));

        assertEquals("INVALID_SCOPE", ex.getCode());
        verifyNoInteractions(userRepository, refreshTokenRepository, auditLog);
    }

    @Test
    void shouldRejectMalformedTargetId() {
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                emergencyLockUseCase.execute(adminId, new EmergencyLockRequest("no-es-un-uuid", "USER", "x"), IP));

        assertEquals("INVALID_ID", ex.getCode());
        verifyNoInteractions(refreshTokenRepository, auditLog);
    }

    @Test
    void shouldFailForUnknownUserWithoutAuditingALockThatDidNotHappen() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                emergencyLockUseCase.execute(adminId, new EmergencyLockRequest(userId.toString(), "USER", "x"), IP));

        verifyNoInteractions(refreshTokenRepository, auditLog);
    }

    private AdminAuditEntry auditEntry() {
        ArgumentCaptor<AdminAuditEntry> entry = ArgumentCaptor.forClass(AdminAuditEntry.class);
        verify(auditLog).log(entry.capture());
        return entry.getValue();
    }
}
