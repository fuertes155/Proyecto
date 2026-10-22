package com.cooperativa.met.application.admin.usecase;

import com.cooperativa.met.application.admin.dto.AdminAuthResponse;
import com.cooperativa.met.application.admin.dto.AdminLoginRequest;
import com.cooperativa.met.domain.admin.model.Admin;
import com.cooperativa.met.domain.admin.model.AdminAuditEntry;
import com.cooperativa.met.domain.admin.model.AdminRole;
import com.cooperativa.met.domain.admin.model.AdminStatus;
import com.cooperativa.met.domain.admin.port.AdminAuditLogPort;
import com.cooperativa.met.domain.admin.port.AdminRepositoryPort;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.identity.port.TokenPort;
import com.cooperativa.met.infrastructure.config.MetSecurityProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminLoginUseCaseTest {

    private static final String IP = "181.50.10.2";

    @Mock
    private AdminRepositoryPort adminRepository;
    @Mock
    private AdminAuditLogPort auditLog;
    @Mock
    private TokenPort tokenPort;
    @Mock
    private PasswordEncoder passwordEncoder;

    private AdminLoginUseCase adminLoginUseCase;

    private final UUID adminId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        MetSecurityProperties securityProperties = new MetSecurityProperties();
        securityProperties.getJwt().setExpirationMs(1_800_000L);
        adminLoginUseCase = new AdminLoginUseCase(adminRepository, auditLog, tokenPort, passwordEncoder, securityProperties);
    }

    @Test
    void shouldIssueTokenWithRoleAndAuditTheLogin() {
        when(adminRepository.findByUsername("oficial")).thenReturn(Optional.of(admin(AdminStatus.ACTIVE)));
        when(passwordEncoder.matches("S3gura!", "bcrypt-hash")).thenReturn(true);
        when(tokenPort.generateAdminAccessToken(adminId, "oficial", "SUPER_ADMIN")).thenReturn("admin-token");

        AdminAuthResponse response = adminLoginUseCase.execute(new AdminLoginRequest("oficial", "S3gura!"), IP);

        assertEquals("admin-token", response.accessToken());
        assertEquals("SUPER_ADMIN", response.role());
        assertEquals(1_800_000L, response.expiresInMs());
        ArgumentCaptor<AdminAuditEntry> entry = ArgumentCaptor.forClass(AdminAuditEntry.class);
        verify(auditLog).log(entry.capture());
        assertEquals("ADMIN_LOGIN", entry.getValue().getAccion());
        assertEquals(adminId, entry.getValue().getActorAdminId());
        assertEquals(IP, entry.getValue().getIpOrigen());
    }

    @Test
    void shouldRejectWrongPasswordWithoutIssuingToken() {
        when(adminRepository.findByUsername("oficial")).thenReturn(Optional.of(admin(AdminStatus.ACTIVE)));
        when(passwordEncoder.matches("adivinada", "bcrypt-hash")).thenReturn(false);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> adminLoginUseCase.execute(new AdminLoginRequest("oficial", "adivinada"), IP));

        assertEquals("INVALID_CREDENTIALS", ex.getCode());
        verifyNoInteractions(tokenPort, auditLog);
    }

    @Test
    void shouldAnswerUnknownUsernameExactlyLikeWrongPassword() {
        when(adminRepository.findByUsername("nadie")).thenReturn(Optional.empty());

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> adminLoginUseCase.execute(new AdminLoginRequest("nadie", "x"), IP));

        assertEquals("INVALID_CREDENTIALS", ex.getCode());
        verifyNoInteractions(tokenPort, passwordEncoder);
    }

    @Test
    void shouldRefuseSuspendedAdmin() {
        when(adminRepository.findByUsername("oficial")).thenReturn(Optional.of(admin(AdminStatus.SUSPENDED)));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> adminLoginUseCase.execute(new AdminLoginRequest("oficial", "S3gura!"), IP));

        assertEquals("ADMIN_SUSPENDED", ex.getCode());
        verify(tokenPort, org.mockito.Mockito.never()).generateAdminAccessToken(any(), any(), any());
    }

    private Admin admin(AdminStatus status) {
        return Admin.builder()
                .id(adminId)
                .username("oficial")
                .passwordHash("bcrypt-hash")
                .fullName("Oficial de Cumplimiento")
                .role(AdminRole.SUPER_ADMIN)
                .status(status)
                .build();
    }
}
