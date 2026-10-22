package com.cooperativa.met.application.admin.usecase;

import com.cooperativa.met.application.admin.dto.OperationLimitRequest;
import com.cooperativa.met.domain.admin.model.AdminAuditEntry;
import com.cooperativa.met.domain.admin.model.OperationLimit;
import com.cooperativa.met.domain.admin.port.AdminAuditLogPort;
import com.cooperativa.met.domain.admin.port.OperationLimitRepositoryPort;
import com.cooperativa.met.domain.common.exception.ResourceNotFoundException;
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
class ManageOperationLimitsUseCaseTest {

    @Mock
    private OperationLimitRepositoryPort limitRepository;
    @Mock
    private AdminAuditLogPort auditLog;

    @InjectMocks
    private ManageOperationLimitsUseCase manageOperationLimitsUseCase;

    private final UUID adminId = UUID.randomUUID();

    @Test
    void shouldUpdateLimitsAndAuditOldAndNewValues() {
        when(limitRepository.findByTipo("TRANSFERENCIA")).thenReturn(Optional.of(OperationLimit.builder()
                .id(UUID.randomUUID()).tipoOperacion("TRANSFERENCIA").montoDiarioMax(5_000_000).montoPorTransaccionMax(2_000_000).activo(true).build()));
        when(limitRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OperationLimit updated = manageOperationLimitsUseCase.update(adminId,
                new OperationLimitRequest("TRANSFERENCIA", 3_000_000L, 1_000_000L), "181.50.10.2");

        assertEquals(3_000_000, updated.getMontoDiarioMax());
        assertEquals(1_000_000, updated.getMontoPorTransaccionMax());
        ArgumentCaptor<AdminAuditEntry> entry = ArgumentCaptor.forClass(AdminAuditEntry.class);
        verify(auditLog).log(entry.capture());
        assertEquals("OPERATION_LIMIT_UPDATED", entry.getValue().getAccion());
        assertEquals("{\"montoDiarioMax\":5000000,\"montoPorTransaccionMax\":2000000}", entry.getValue().getValoresAnteriores());
        assertEquals("{\"montoDiarioMax\":3000000,\"montoPorTransaccionMax\":1000000}", entry.getValue().getValoresNuevos());
    }

    @Test
    void shouldFailForUnknownOperationType() {
        when(limitRepository.findByTipo("NO_EXISTE")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> manageOperationLimitsUseCase.update(adminId,
                new OperationLimitRequest("NO_EXISTE", 1L, 1L), "181.50.10.2"));

        verify(limitRepository, never()).save(any());
        verifyNoInteractions(auditLog);
    }
}
