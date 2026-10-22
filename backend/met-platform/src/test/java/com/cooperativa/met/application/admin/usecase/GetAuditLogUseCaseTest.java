package com.cooperativa.met.application.admin.usecase;

import com.cooperativa.met.domain.admin.model.AdminAuditEntry;
import com.cooperativa.met.domain.admin.port.AdminAuditLogPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAuditLogUseCaseTest {

    @Mock
    private AdminAuditLogPort auditLog;

    @InjectMocks
    private GetAuditLogUseCase getAuditLogUseCase;

    @Test
    void shouldFilterByAdmin() {
        UUID adminId = UUID.randomUUID();
        List<AdminAuditEntry> entries = List.of(AdminAuditEntry.builder().actorAdminId(adminId).accion("ADMIN_LOGIN").build());
        when(auditLog.findByAdminId(adminId, 0, 20)).thenReturn(entries);

        assertEquals(entries, getAuditLogUseCase.getByAdmin(adminId.toString(), 0, 20));
    }

    @Test
    void shouldReturnNothingForMalformedAdminIdInsteadOfFailing() {
        assertTrue(getAuditLogUseCase.getByAdmin("'; DROP TABLE admin_audit_log; --", 0, 20).isEmpty());

        verifyNoInteractions(auditLog);
    }
}
