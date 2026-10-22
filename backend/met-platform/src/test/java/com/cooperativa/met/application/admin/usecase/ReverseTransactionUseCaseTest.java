package com.cooperativa.met.application.admin.usecase;

import com.cooperativa.met.application.admin.dto.ReverseTransactionRequest;
import com.cooperativa.met.domain.admin.model.AdminAuditEntry;
import com.cooperativa.met.domain.admin.port.AdminAuditLogPort;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ReverseTransactionUseCaseTest {

    private static final String TX_ID = "a1b2c3d4-0000-4000-8000-000000000001";

    @Mock
    private AdminAuditLogPort auditLog;

    @InjectMocks
    private ReverseTransactionUseCase reverseTransactionUseCase;

    private final UUID adminId = UUID.randomUUID();

    @Test
    void shouldAuditReversalWhenConfirmationCodeMatches() {
        reverseTransactionUseCase.execute(adminId,
                new ReverseTransactionRequest(TX_ID, "Cobro duplicado", "confirm-a1b2c3d4"), "181.50.10.2");

        ArgumentCaptor<AdminAuditEntry> entry = ArgumentCaptor.forClass(AdminAuditEntry.class);
        verify(auditLog).log(entry.capture());
        assertEquals("TRANSACTION_REVERSED", entry.getValue().getAccion());
        assertEquals(TX_ID, entry.getValue().getIdEntidad());
        assertEquals("Cobro duplicado", entry.getValue().getMotivo());
        assertEquals(adminId, entry.getValue().getActorAdminId());
    }

    @Test
    void shouldRejectWrongConfirmationCodeAndLeaveNoTrace() {
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> reverseTransactionUseCase.execute(adminId,
                new ReverseTransactionRequest(TX_ID, "Cobro duplicado", "CONFIRM-FFFFFFFF"), "181.50.10.2"));

        assertEquals("INVALID_CONFIRMATION_CODE", ex.getCode());
        verifyNoInteractions(auditLog);
    }
}
