package com.cooperativa.met.application.admin.usecase;

import com.cooperativa.met.application.admin.dto.FeeScheduleRequest;
import com.cooperativa.met.domain.admin.model.AdminAuditEntry;
import com.cooperativa.met.domain.admin.model.FeeSchedule;
import com.cooperativa.met.domain.admin.port.AdminAuditLogPort;
import com.cooperativa.met.domain.admin.port.FeeScheduleRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManageFeesUseCaseTest {

    private static final String IP = "181.50.10.2";
    private static final Instant DESDE = Instant.parse("2026-11-01T05:00:00Z");

    @Mock
    private FeeScheduleRepositoryPort feeRepository;
    @Mock
    private AdminAuditLogPort auditLog;

    @InjectMocks
    private ManageFeesUseCase manageFeesUseCase;

    private final UUID adminId = UUID.randomUUID();

    @Test
    void shouldCloseCurrentVersionBeforeOpeningTheNewOne() {
        when(feeRepository.save(any())).thenAnswer(inv -> withId(inv.getArgument(0), UUID.randomUUID()));

        FeeSchedule created = manageFeesUseCase.create(adminId,
                new FeeScheduleRequest("RETIRO_PSE", "Comisión retiro", new BigDecimal("2500"), false, DESDE), IP);

        InOrder order = inOrder(feeRepository);
        order.verify(feeRepository).cerrarVigencia("RETIRO_PSE");
        order.verify(feeRepository).save(any());
        assertNull(created.getVigentaHasta(), "la versión nueva queda abierta hasta que se cree la siguiente");
        assertEquals(adminId, created.getCreadoPor());
        assertEquals(new BigDecimal("2500"), created.getValor());

        AdminAuditEntry entry = auditEntry();
        assertEquals("FEE_SCHEDULE_CREATED", entry.getAccion());
        assertEquals(created.getId().toString(), entry.getIdEntidad());
        assertTrue(entry.getValoresNuevos().contains("\"valor\":2500"), entry.getValoresNuevos());
    }

    @Test
    void shouldKeepFeeTypeAndDescriptionWhenUpdatingOnlyTheValue() {
        UUID feeId = UUID.randomUUID();
        when(feeRepository.findById(feeId)).thenReturn(Optional.of(existingFee(feeId)));
        when(feeRepository.save(any())).thenAnswer(inv -> withId(inv.getArgument(0), UUID.randomUUID()));

        FeeSchedule updated = manageFeesUseCase.update(adminId, feeId,
                new FeeScheduleRequest("OTRO_TIPO", null, new BigDecimal("1.5"), true, DESDE), IP);

        assertEquals("RETIRO_PSE", updated.getTipoTarifa(), "el tipo de tarifa no se puede cambiar al editar");
        assertEquals("Comisión retiro", updated.getDescripcion());
        assertEquals(new BigDecimal("1.5"), updated.getValor());
        assertTrue(updated.isEsPorcentaje());
        verify(feeRepository).deleteById(feeId);

        AdminAuditEntry entry = auditEntry();
        assertEquals("FEE_SCHEDULE_UPDATED", entry.getAccion());
        assertEquals("{\"valor\":2500,\"esPorcentaje\":false}", entry.getValoresAnteriores());
    }

    @Test
    void shouldAuditDeletedFee() {
        UUID feeId = UUID.randomUUID();
        when(feeRepository.findById(feeId)).thenReturn(Optional.of(existingFee(feeId)));

        manageFeesUseCase.delete(adminId, feeId, IP);

        verify(feeRepository).deleteById(feeId);
        AdminAuditEntry entry = auditEntry();
        assertEquals("FEE_SCHEDULE_DELETED", entry.getAccion());
        assertTrue(entry.getValoresAnteriores().contains("RETIRO_PSE"));
    }

    @Test
    void shouldFailToUpdateOrDeleteUnknownFee() {
        UUID feeId = UUID.randomUUID();
        when(feeRepository.findById(feeId)).thenReturn(Optional.empty());
        FeeScheduleRequest request = new FeeScheduleRequest("RETIRO_PSE", null, BigDecimal.ONE, false, DESDE);

        assertThrows(IllegalArgumentException.class, () -> manageFeesUseCase.update(adminId, feeId, request, IP));
        assertThrows(IllegalArgumentException.class, () -> manageFeesUseCase.delete(adminId, feeId, IP));

        verify(feeRepository, never()).deleteById(any());
        verifyNoInteractions(auditLog);
    }

    private FeeSchedule existingFee(UUID id) {
        return FeeSchedule.builder()
                .id(id)
                .tipoTarifa("RETIRO_PSE")
                .descripcion("Comisión retiro")
                .valor(new BigDecimal("2500"))
                .esPorcentaje(false)
                .vigentDesde(Instant.parse("2026-01-01T05:00:00Z"))
                .build();
    }

    private static FeeSchedule withId(FeeSchedule fee, UUID id) {
        return FeeSchedule.builder()
                .id(id)
                .tipoTarifa(fee.getTipoTarifa())
                .descripcion(fee.getDescripcion())
                .valor(fee.getValor())
                .esPorcentaje(fee.isEsPorcentaje())
                .vigentDesde(fee.getVigentDesde())
                .vigentaHasta(fee.getVigentaHasta())
                .creadoPor(fee.getCreadoPor())
                .createdAt(fee.getCreatedAt())
                .build();
    }

    private AdminAuditEntry auditEntry() {
        ArgumentCaptor<AdminAuditEntry> entry = ArgumentCaptor.forClass(AdminAuditEntry.class);
        verify(auditLog).log(entry.capture());
        return entry.getValue();
    }
}
