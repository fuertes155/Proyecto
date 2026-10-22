package com.cooperativa.met.infrastructure.persistence.admin;

import com.cooperativa.met.domain.admin.model.Admin;
import com.cooperativa.met.domain.admin.model.AdminAuditEntry;
import com.cooperativa.met.domain.admin.model.AdminRole;
import com.cooperativa.met.domain.admin.model.AdminStatus;
import com.cooperativa.met.domain.admin.model.FeeSchedule;
import com.cooperativa.met.domain.admin.model.MaintenanceWindow;
import com.cooperativa.met.domain.admin.model.OperationLimit;
import com.cooperativa.met.domain.admin.model.PlatformRevenue;
import com.cooperativa.met.domain.admin.model.RiskAction;
import com.cooperativa.met.domain.admin.model.RiskRule;
import com.cooperativa.met.domain.admin.port.AdminAuditLogPort;
import com.cooperativa.met.domain.admin.port.AdminRepositoryPort;
import com.cooperativa.met.domain.admin.port.FeeScheduleRepositoryPort;
import com.cooperativa.met.domain.admin.port.MaintenanceRepositoryPort;
import com.cooperativa.met.domain.admin.port.OperationLimitRepositoryPort;
import com.cooperativa.met.domain.admin.port.PlatformRevenuePort;
import com.cooperativa.met.domain.admin.port.RiskRuleRepositoryPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Adaptadores de persistencia del panel admin contra H2: mapeo dominio ↔ entidad y consultas propias. */
@SpringBootTest
@org.springframework.test.context.ActiveProfiles("test")
@org.springframework.context.annotation.Import(com.cooperativa.met.TestRedisConfig.class)
class AdminPersistenceIntegrationTest {

    @Autowired private AdminRepositoryPort adminRepository;
    @Autowired private AdminAuditLogPort auditLog;
    @Autowired private FeeScheduleRepositoryPort feeRepository;
    @Autowired private MaintenanceRepositoryPort maintenanceRepository;
    @Autowired private OperationLimitRepositoryPort limitRepository;
    @Autowired private RiskRuleRepositoryPort riskRuleRepository;
    @Autowired private PlatformRevenuePort platformRevenuePort;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);

    @Test
    void shouldStoreAndFindAdmins() {
        Admin saved = adminRepository.save(Admin.builder()
                .id(UUID.randomUUID())
                .username("oficial-" + suffix)
                .passwordHash("$2a$10$hash")
                .fullName("Oficial de Cumplimiento")
                .email("oficial-" + suffix + "@coop.co")
                .role(AdminRole.SUPER_ADMIN)
                .status(AdminStatus.ACTIVE)
                .createdAt(Instant.now())
                .build());

        Admin byUsername = adminRepository.findByUsername("oficial-" + suffix).orElseThrow();
        assertEquals(saved.getId(), byUsername.getId());
        assertEquals(AdminRole.SUPER_ADMIN, byUsername.getRole());
        assertTrue(byUsername.isActive());
        assertEquals("$2a$10$hash", adminRepository.findById(saved.getId()).orElseThrow().getPasswordHash());
        assertTrue(adminRepository.findAll().stream().anyMatch(a -> a.getId().equals(saved.getId())));
    }

    @Test
    void shouldPageAuditLogNewestFirstAndPurgeOldEntries() {
        UUID adminId = UUID.randomUUID();
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        auditLog.log(entry(adminId, "VIEJA", now.minus(Duration.ofDays(400))));
        auditLog.log(entry(adminId, "MEDIA", now.minus(Duration.ofDays(1))));
        auditLog.log(entry(adminId, "NUEVA", now));
        long before = auditLog.countAll();

        assertEquals(List.of("NUEVA", "MEDIA"), auditLog.findByAdminId(adminId, 0, 2).stream().map(AdminAuditEntry::getAccion).toList());
        assertEquals(List.of("VIEJA"), auditLog.findByAdminId(adminId, 1, 2).stream().map(AdminAuditEntry::getAccion).toList());
        assertEquals(3, auditLog.findByEntidad("USER", adminId.toString(), 0, 10).size());
        assertFalse(auditLog.findAll(0, 5).isEmpty());

        auditLog.purgeOldLogs(now.minus(Duration.ofDays(365)));

        assertEquals(List.of("NUEVA", "MEDIA"), auditLog.findByAdminId(adminId, 0, 10).stream().map(AdminAuditEntry::getAccion).toList());
        assertTrue(auditLog.countAll() <= before - 1);
    }

    @Test
    void shouldVersionFeesClosingTheCurrentOne() {
        String tipo = "TEST_FEE_" + suffix;
        FeeSchedule first = feeRepository.save(fee(tipo, "2500"));
        assertTrue(feeRepository.findVigentes().stream().anyMatch(f -> f.getId().equals(first.getId())));

        feeRepository.cerrarVigencia(tipo);
        FeeSchedule second = feeRepository.save(fee(tipo, "3000"));

        List<FeeSchedule> vigentes = feeRepository.findVigentes().stream().filter(f -> f.getTipoTarifa().equals(tipo)).toList();
        assertEquals(List.of(second.getId()), vigentes.stream().map(FeeSchedule::getId).toList());
        assertNotNull(feeRepository.findById(first.getId()).orElseThrow().getVigentaHasta());
        assertEquals(0, new BigDecimal("3000").compareTo(vigentes.get(0).getValor()));

        feeRepository.deleteById(second.getId());
        assertTrue(feeRepository.findById(second.getId()).isEmpty());
        assertTrue(feeRepository.findAll().stream().anyMatch(f -> f.getId().equals(first.getId())));
    }

    @Test
    void shouldKeepMaintenanceEndpointsWhenStoringWindows() {
        Instant inicio = Instant.now().plus(Duration.ofDays(1)).truncatedTo(ChronoUnit.SECONDS);
        MaintenanceWindow saved = maintenanceRepository.save(MaintenanceWindow.builder()
                .descripcion("Migración " + suffix)
                .inicio(inicio)
                .fin(inicio.plus(Duration.ofHours(2)))
                .activo(false)
                .endpointsActivos(List.of("/v1/health", "/v1/auth/login"))
                .creadoPor(UUID.randomUUID())
                .build());

        MaintenanceWindow found = maintenanceRepository.findById(saved.getId()).orElseThrow();
        assertEquals(List.of("/v1/health", "/v1/auth/login"), found.getEndpointsActivos());
        assertEquals(inicio, found.getInicio());
        assertTrue(maintenanceRepository.findAll().stream().anyMatch(w -> w.getId().equals(saved.getId())));

        maintenanceRepository.save(found.activate());
        assertEquals(saved.getId(), maintenanceRepository.findActiva().orElseThrow().getId());
        maintenanceRepository.save(found.deactivate());
        maintenanceRepository.deleteById(saved.getId());
        assertTrue(maintenanceRepository.findById(saved.getId()).isEmpty());
    }

    @Test
    void shouldUpdateOperationLimitsByType() {
        String tipo = "TEST_OP_" + suffix;
        OperationLimit saved = limitRepository.save(OperationLimit.builder()
                .id(UUID.randomUUID()).tipoOperacion(tipo).montoDiarioMax(5_000_000).montoPorTransaccionMax(2_000_000)
                .activo(true).updatedAt(Instant.now()).build());

        limitRepository.save(saved.withLimits(3_000_000, 1_000_000));

        OperationLimit found = limitRepository.findByTipo(tipo).orElseThrow();
        assertEquals(3_000_000, found.getMontoDiarioMax());
        assertEquals(1_000_000, found.getMontoPorTransaccionMax());
        assertTrue(limitRepository.findAll().stream().anyMatch(l -> l.getTipoOperacion().equals(tipo)));
    }

    @Test
    void shouldListOnlyActiveRiskRulesAsActive() {
        RiskRule on = riskRuleRepository.save(rule("Montos altos " + suffix, true));
        RiskRule off = riskRuleRepository.save(rule("Regla vieja " + suffix, false));

        List<UUID> activas = riskRuleRepository.findActivas().stream().map(RiskRule::getId).toList();
        assertTrue(activas.contains(on.getId()));
        assertFalse(activas.contains(off.getId()));
        assertEquals(RiskAction.REVIEW, riskRuleRepository.findById(on.getId()).orElseThrow().getAccion());
        assertEquals(2, riskRuleRepository.findAll().stream().filter(r -> r.getNombre().endsWith(suffix)).count());

        riskRuleRepository.deleteById(off.getId());
        assertTrue(riskRuleRepository.findById(off.getId()).isEmpty());
    }

    @Test
    void shouldStorePlatformRevenue() {
        PlatformRevenue saved = platformRevenuePort.save(PlatformRevenue.builder()
                .id(UUID.randomUUID()).userId(UUID.randomUUID()).amount(new BigDecimal("1500.00"))
                .description("Comisión").source("SCHEDULED_SAVINGS_DEPOSIT").createdAt(Instant.now()).build());

        assertNotNull(saved);
        assertEquals(0, new BigDecimal("1500.00").compareTo(saved.getAmount()));
    }

    private AdminAuditEntry entry(UUID adminId, String accion, Instant timestamp) {
        return AdminAuditEntry.builder()
                .actorAdminId(adminId)
                .accion(accion)
                .entidadAfectada("USER")
                .idEntidad(adminId.toString())
                .valoresNuevos("{}")
                .motivo("prueba")
                .ipOrigen("127.0.0.1")
                .timestamp(timestamp)
                .build();
    }

    private FeeSchedule fee(String tipo, String valor) {
        return FeeSchedule.builder()
                .tipoTarifa(tipo)
                .descripcion("Tarifa de prueba")
                .valor(new BigDecimal(valor))
                .esPorcentaje(false)
                .vigentDesde(Instant.now().minus(Duration.ofMinutes(1)))
                .creadoPor(UUID.randomUUID())
                .build();
    }

    private RiskRule rule(String nombre, boolean activo) {
        return RiskRule.builder()
                .nombre(nombre)
                .descripcion("prueba")
                .condicion("{\"field\":\"amount\",\"operator\":\"gt\",\"value\":10000000}")
                .accion(RiskAction.REVIEW)
                .activo(activo)
                .creadoPor(UUID.randomUUID())
                .createdAt(Instant.now())
                .build();
    }
}
