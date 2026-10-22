package com.cooperativa.met.infrastructure.web.admin;

import com.cooperativa.met.application.admin.dto.MaintenanceWindowRequest;
import com.cooperativa.met.application.admin.usecase.ManageMaintenanceUseCase;
import com.cooperativa.met.application.compliance.dto.ComplianceAlertResponse;
import com.cooperativa.met.application.compliance.usecase.ListComplianceAlertsUseCase;
import com.cooperativa.met.application.compliance.usecase.ListRestrictiveListMatchesUseCase;
import com.cooperativa.met.application.compliance.usecase.RefreshRestrictiveListsUseCase;
import com.cooperativa.met.application.compliance.usecase.ReviewComplianceAlertUseCase;
import com.cooperativa.met.application.compliance.usecase.TriggerRestrictiveListRefreshUseCase;
import com.cooperativa.met.application.investment.usecase.ListInvestmentInstrumentsUseCase;
import com.cooperativa.met.domain.admin.model.MaintenanceWindow;
import com.cooperativa.met.domain.compliance.model.AlertStatus;
import com.cooperativa.met.domain.identity.model.ComplianceListType;
import com.cooperativa.met.domain.investment.model.InvestmentInstrument;
import com.cooperativa.met.domain.investment.port.InvestmentInstrumentPort;
import com.cooperativa.met.infrastructure.config.RateLimitProperties;
import com.cooperativa.met.infrastructure.web.common.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Controladores admin de cumplimiento, mantenimiento e instrumentos de inversión (MockMvc standalone). */
@ExtendWith(MockitoExtension.class)
class AdminOperationsControllersTest {

    @Mock private ListComplianceAlertsUseCase listAlertsUseCase;
    @Mock private ReviewComplianceAlertUseCase reviewAlertUseCase;
    @Mock private ListRestrictiveListMatchesUseCase listMatchesUseCase;
    @Mock private TriggerRestrictiveListRefreshUseCase triggerRefreshUseCase;
    @Mock private ManageMaintenanceUseCase maintenanceUseCase;
    @Mock private InvestmentInstrumentPort instrumentPort;
    @Mock private ListInvestmentInstrumentsUseCase listInstrumentsUseCase;

    private MockMvc mvc;
    private final UUID adminId = UUID.randomUUID();
    private final UsernamePasswordAuthenticationToken admin = new UsernamePasswordAuthenticationToken(adminId, null, List.of());

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(
                        new ComplianceController(listAlertsUseCase, reviewAlertUseCase, listMatchesUseCase, triggerRefreshUseCase),
                        new MaintenanceController(maintenanceUseCase),
                        new InvestmentInstrumentsAdminController(instrumentPort, listInstrumentsUseCase))
                .setControllerAdvice(new GlobalExceptionHandler(new RateLimitProperties()))
                .build();
    }

    @Test
    void shouldPageAlertsByStatusWithTheirTotal() throws Exception {
        when(listAlertsUseCase.execute(AlertStatus.UNDER_REVIEW, 1, 5)).thenReturn(List.of(alert()));
        when(listAlertsUseCase.countByStatus(AlertStatus.UNDER_REVIEW)).thenReturn(6L);

        mvc.perform(get("/v1/admin/compliance/alerts").param("status", "under_review").param("page", "1").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(6))
                .andExpect(jsonPath("$.items[0].alertType").value("UNUSUAL_AMOUNT"));
    }

    @Test
    void shouldRejectUnknownAlertStatus() throws Exception {
        mvc.perform(get("/v1/admin/compliance/alerts").param("status", "CERRADA"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS"));

        verifyNoInteractions(listAlertsUseCase);
    }

    @Test
    void shouldReviewAlertAsTheAuthenticatedOfficerFromTheirIp() throws Exception {
        UUID alertId = UUID.randomUUID();
        when(reviewAlertUseCase.execute(adminId, alertId, AlertStatus.REPORTED, "A la UIAF", "10.1.2.3")).thenReturn(alert());

        mvc.perform(post("/v1/admin/compliance/alerts/{id}/review", alertId).principal(admin)
                        .with(request -> { request.setRemoteAddr("10.1.2.3"); return request; })
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"reported\",\"notes\":\"A la UIAF\"}"))
                .andExpect(status().isOk());

        verify(reviewAlertUseCase).execute(adminId, alertId, AlertStatus.REPORTED, "A la UIAF", "10.1.2.3");
    }

    @Test
    void shouldTriggerListRefreshAndListMatches() throws Exception {
        Map<ComplianceListType, RefreshRestrictiveListsUseCase.RefreshResult> results = new LinkedHashMap<>();
        results.put(ComplianceListType.OFAC, new RefreshRestrictiveListsUseCase.RefreshResult(ComplianceListType.OFAC, true, 12000, null));
        when(triggerRefreshUseCase.execute(eq(adminId), any())).thenReturn(results);
        when(listMatchesUseCase.execute(0, 20)).thenReturn(List.of());

        mvc.perform(post("/v1/admin/compliance/restrictive-lists/refresh").principal(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.OFAC.entriesLoaded").value(12000));
        mvc.perform(get("/v1/admin/compliance/restrictive-list-matches")).andExpect(status().isOk());
    }

    @Test
    void shouldScheduleActivateAndListMaintenanceWindows() throws Exception {
        UUID windowId = UUID.randomUUID();
        MaintenanceWindow window = MaintenanceWindow.builder().id(windowId).descripcion("Migración").activo(true)
                .inicio(Instant.now().minusSeconds(60)).fin(Instant.now().plusSeconds(3600)).build();
        when(maintenanceUseCase.schedule(eq(adminId), any(MaintenanceWindowRequest.class), any())).thenReturn(window);
        when(maintenanceUseCase.activate(eq(adminId), eq(windowId), any())).thenReturn(window);
        when(maintenanceUseCase.deactivate(eq(adminId), eq(windowId), any())).thenReturn(window);
        when(maintenanceUseCase.getAll()).thenReturn(List.of(window));
        when(maintenanceUseCase.getActiva()).thenReturn(Optional.of(window));
        String inicio = Instant.now().plusSeconds(3600).toString();
        String fin = Instant.now().plusSeconds(7200).toString();

        mvc.perform(post("/v1/admin/maintenance").principal(admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descripcion\":\"Migración\",\"inicio\":\"" + inicio + "\",\"fin\":\"" + fin + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.descripcion").value("Migración"));
        mvc.perform(patch("/v1/admin/maintenance/{id}/activate", windowId).principal(admin)).andExpect(status().isOk());
        mvc.perform(patch("/v1/admin/maintenance/{id}/deactivate", windowId).principal(admin)).andExpect(status().isOk());
        mvc.perform(get("/v1/admin/maintenance")).andExpect(status().isOk()).andExpect(jsonPath("$[0].activo").value(true));
        mvc.perform(get("/v1/admin/maintenance/activa")).andExpect(status().isOk());
    }

    @Test
    void shouldRejectMaintenanceThatEndsInThePast() throws Exception {
        mvc.perform(post("/v1/admin/maintenance").principal(admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descripcion\":\"x\",\"inicio\":\"2020-01-01T00:00:00Z\",\"fin\":\"2020-01-02T00:00:00Z\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.fin").exists());

        verifyNoInteractions(maintenanceUseCase);
    }

    @Test
    void shouldCreateToggleAndDeleteInvestmentInstruments() throws Exception {
        UUID instrumentId = UUID.randomUUID();
        when(instrumentPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(instrumentPort.findById(instrumentId)).thenReturn(Optional.of(InvestmentInstrument.builder()
                .id(instrumentId).nombre("CDT").tasaAnual(new BigDecimal("0.09")).activo(true).build()));

        mvc.perform(post("/v1/admin/investment-instruments").principal(admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"CDT 180\",\"tasaAnual\":0.105,\"plazoDias\":180,\"montoMinimo\":50000}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.activo").value(true));
        mvc.perform(patch("/v1/admin/investment-instruments/{id}/toggle", instrumentId).param("activo", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
        mvc.perform(delete("/v1/admin/investment-instruments/{id}", instrumentId)).andExpect(status().isOk());

        ArgumentCaptor<InvestmentInstrument> created = ArgumentCaptor.forClass(InvestmentInstrument.class);
        verify(instrumentPort, org.mockito.Mockito.times(2)).save(created.capture());
        assertEquals(adminId, created.getAllValues().get(0).getCreadoPor());
        verify(instrumentPort).deleteById(instrumentId);
    }

    @Test
    void shouldRejectInstrumentsWithoutRateOrBelowMinimumAmount() throws Exception {
        mvc.perform(post("/v1/admin/investment-instruments").principal(admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"CDT\",\"plazoDias\":0,\"montoMinimo\":500}"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("tasaAnual")));

        verify(instrumentPort, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void shouldAnswer400ForTogglingAnUnknownInstrument() throws Exception {
        UUID unknown = UUID.randomUUID();
        when(instrumentPort.findById(unknown)).thenReturn(Optional.empty());

        mvc.perform(patch("/v1/admin/investment-instruments/{id}/toggle", unknown).param("activo", "true"))
                .andExpect(status().isBadRequest());
    }

    private ComplianceAlertResponse alert() {
        return new ComplianceAlertResponse(UUID.randomUUID(), UUID.randomUUID(), "Ana Pérez", "52123456", UUID.randomUUID(),
                "UNUSUAL_AMOUNT", "HIGH", "Monto inusual", "OPEN", Instant.now(), null, null, null);
    }
}
