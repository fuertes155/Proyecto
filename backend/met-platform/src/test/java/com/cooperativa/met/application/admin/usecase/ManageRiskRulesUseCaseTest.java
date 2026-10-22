package com.cooperativa.met.application.admin.usecase;

import com.cooperativa.met.application.admin.dto.RiskRuleRequest;
import com.cooperativa.met.domain.admin.model.AdminAuditEntry;
import com.cooperativa.met.domain.admin.model.RiskAction;
import com.cooperativa.met.domain.admin.model.RiskRule;
import com.cooperativa.met.domain.admin.port.AdminAuditLogPort;
import com.cooperativa.met.domain.admin.port.RiskRuleRepositoryPort;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManageRiskRulesUseCaseTest {

    private static final String IP = "181.50.10.2";
    private static final String CONDICION = "{\"field\":\"amount\",\"operator\":\"gt\",\"value\":10000000}";

    @Mock
    private RiskRuleRepositoryPort riskRuleRepository;
    @Mock
    private AdminAuditLogPort auditLog;

    @InjectMocks
    private ManageRiskRulesUseCase manageRiskRulesUseCase;

    private final UUID adminId = UUID.randomUUID();
    private final UUID ruleId = UUID.randomUUID();

    @Test
    void shouldCreateRuleAcceptingActionInAnyCase() {
        when(riskRuleRepository.save(any())).thenAnswer(inv -> ((RiskRule) inv.getArgument(0)).toBuilder().id(ruleId).build());

        RiskRule created = manageRiskRulesUseCase.create(adminId,
                new RiskRuleRequest("Montos altos", "Revisión manual", CONDICION, "review", true), IP);

        assertEquals(RiskAction.REVIEW, created.getAccion());
        assertTrue(created.isActivo());
        assertEquals(adminId, created.getCreadoPor());
        AdminAuditEntry entry = auditEntry();
        assertEquals("RISK_RULE_CREATED", entry.getAccion());
        assertEquals(ruleId.toString(), entry.getIdEntidad());
    }

    @Test
    void shouldRejectUnknownActionWithoutSavingAnything() {
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> manageRiskRulesUseCase.create(adminId,
                new RiskRuleRequest("Montos altos", null, CONDICION, "APROBAR", true), IP));

        assertEquals("INVALID_ACTION", ex.getCode());
        verify(riskRuleRepository, never()).save(any());
        verifyNoInteractions(auditLog);
    }

    @Test
    void shouldAuditWhoDisabledARule() {
        when(riskRuleRepository.findById(ruleId)).thenReturn(Optional.of(rule(true)));
        when(riskRuleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RiskRule updated = manageRiskRulesUseCase.toggleActivo(adminId, ruleId, false, IP);

        assertFalse(updated.isActivo());
        AdminAuditEntry entry = auditEntry();
        assertEquals("RISK_RULE_DISABLED", entry.getAccion());
        assertEquals(adminId, entry.getActorAdminId());
        assertEquals("{\"activo\":true}", entry.getValoresAnteriores());
        assertEquals("{\"activo\":false}", entry.getValoresNuevos());
    }

    @Test
    void shouldAuditDeletedRule() {
        when(riskRuleRepository.findById(ruleId)).thenReturn(Optional.of(rule(true)));

        manageRiskRulesUseCase.delete(adminId, ruleId, IP);

        verify(riskRuleRepository).deleteById(ruleId);
        AdminAuditEntry entry = auditEntry();
        assertEquals("RISK_RULE_DELETED", entry.getAccion());
        assertTrue(entry.getValoresAnteriores().contains("Montos altos"));
    }

    @Test
    void shouldFailForUnknownRule() {
        when(riskRuleRepository.findById(ruleId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> manageRiskRulesUseCase.toggleActivo(adminId, ruleId, true, IP));
        assertThrows(ResourceNotFoundException.class, () -> manageRiskRulesUseCase.delete(adminId, ruleId, IP));

        verify(riskRuleRepository, never()).deleteById(any());
        verifyNoInteractions(auditLog);
    }

    private RiskRule rule(boolean activo) {
        return RiskRule.builder().id(ruleId).nombre("Montos altos").condicion(CONDICION).accion(RiskAction.REVIEW).activo(activo).build();
    }

    private AdminAuditEntry auditEntry() {
        ArgumentCaptor<AdminAuditEntry> entry = ArgumentCaptor.forClass(AdminAuditEntry.class);
        verify(auditLog).log(entry.capture());
        return entry.getValue();
    }
}
