package com.cooperativa.met.application.admin.usecase;

import com.cooperativa.met.application.admin.dto.RiskRuleRequest;
import com.cooperativa.met.domain.admin.model.AdminAuditEntry;
import com.cooperativa.met.domain.admin.model.RiskAction;
import com.cooperativa.met.domain.admin.model.RiskRule;
import com.cooperativa.met.domain.admin.port.AdminAuditLogPort;
import com.cooperativa.met.domain.admin.port.RiskRuleRepositoryPort;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ManageRiskRulesUseCase {

    private final RiskRuleRepositoryPort riskRuleRepository;
    private final AdminAuditLogPort auditLog;

    public List<RiskRule> getAll() {
        return riskRuleRepository.findAll();
    }

    public List<RiskRule> getActivas() {
        return riskRuleRepository.findActivas();
    }

    @Transactional
    public RiskRule create(UUID actorAdminId, RiskRuleRequest request, String ip) {
        RiskAction accion = parseAccion(request.accion());

        RiskRule rule = RiskRule.builder()
                .nombre(request.nombre())
                .descripcion(request.descripcion())
                .condicion(request.condicion())
                .accion(accion)
                .activo(request.activo())
                .creadoPor(actorAdminId)
                .createdAt(Instant.now())
                .build();

        RiskRule saved = riskRuleRepository.save(rule);

        auditLog.log(AdminAuditEntry.builder()
                .actorAdminId(actorAdminId)
                .accion("RISK_RULE_CREATED")
                .entidadAfectada("RISK_RULE")
                .idEntidad(saved.getId().toString())
                .valoresNuevos(String.format("{\"nombre\":\"%s\",\"accion\":\"%s\"}", request.nombre(), request.accion()))
                .motivo("Creación de regla de riesgo")
                .ipOrigen(ip)
                .timestamp(Instant.now())
                .build());

        return saved;
    }

    @Transactional
    public RiskRule toggleActivo(UUID actorAdminId, UUID ruleId, boolean activo, String ip) {
        RiskRule rule = riskRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Regla de riesgo no encontrada: " + ruleId));

        boolean prevActivo = rule.isActivo();
        RiskRule updated = riskRuleRepository.save(rule.withActivo(activo));

        auditLog.log(AdminAuditEntry.builder()
                .actorAdminId(actorAdminId)
                .accion(activo ? "RISK_RULE_ENABLED" : "RISK_RULE_DISABLED")
                .entidadAfectada("RISK_RULE")
                .idEntidad(ruleId.toString())
                .valoresAnteriores(String.format("{\"activo\":%s}", prevActivo))
                .valoresNuevos(String.format("{\"activo\":%s}", activo))
                .ipOrigen(ip)
                .timestamp(Instant.now())
                .build());

        return updated;
    }

    @Transactional
    public void delete(UUID actorAdminId, UUID ruleId, String ip) {
        RiskRule rule = riskRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Regla de riesgo no encontrada: " + ruleId));
        riskRuleRepository.deleteById(ruleId);

        auditLog.log(AdminAuditEntry.builder()
                .actorAdminId(actorAdminId)
                .accion("RISK_RULE_DELETED")
                .entidadAfectada("RISK_RULE")
                .idEntidad(ruleId.toString())
                .valoresAnteriores(String.format("{\"nombre\":\"%s\"}", rule.getNombre()))
                .ipOrigen(ip)
                .timestamp(Instant.now())
                .build());
    }

    private RiskAction parseAccion(String accion) {
        try {
            return RiskAction.valueOf(accion.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("INVALID_ACTION",
                    "Acción inválida. Use: ALLOW, BLOCK, REVIEW");
        }
    }
}
