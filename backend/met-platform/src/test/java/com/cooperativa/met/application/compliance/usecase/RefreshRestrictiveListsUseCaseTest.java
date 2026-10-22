package com.cooperativa.met.application.compliance.usecase;

import com.cooperativa.met.domain.admin.model.AdminAuditEntry;
import com.cooperativa.met.domain.admin.port.AdminAuditLogPort;
import com.cooperativa.met.domain.compliance.model.RestrictiveListEntry;
import com.cooperativa.met.domain.compliance.port.RestrictiveListProviderPort;
import com.cooperativa.met.domain.compliance.port.RestrictiveListRepositoryPort;
import com.cooperativa.met.domain.identity.model.ComplianceListType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshRestrictiveListsUseCaseTest {

    @Mock
    private RestrictiveListProviderPort ofacProvider;
    @Mock
    private RestrictiveListProviderPort onuProvider;
    @Mock
    private RestrictiveListRepositoryPort repository;
    @Mock
    private AdminAuditLogPort auditLog;

    private RefreshRestrictiveListsUseCase refreshUseCase;

    @BeforeEach
    void setUp() {
        when(ofacProvider.getListType()).thenReturn(ComplianceListType.OFAC);
        when(onuProvider.getListType()).thenReturn(ComplianceListType.ONU);
        refreshUseCase = new RefreshRestrictiveListsUseCase(List.of(ofacProvider, onuProvider), repository);
    }

    @Test
    void shouldReplaceEachListCompletely() {
        List<RestrictiveListEntry> ofac = List.of(entry(ComplianceListType.OFAC, "PABLO EMILIO"), entry(ComplianceListType.OFAC, "JUAN PEREZ"));
        when(ofacProvider.fetchEntries()).thenReturn(ofac);
        when(onuProvider.fetchEntries()).thenReturn(List.of());

        Map<ComplianceListType, RefreshRestrictiveListsUseCase.RefreshResult> results = refreshUseCase.execute();

        verify(repository).deleteByListType(ComplianceListType.OFAC);
        verify(repository).saveAll(ofac);
        assertEquals(2, results.get(ComplianceListType.OFAC).entriesLoaded());
        assertTrue(results.get(ComplianceListType.ONU).success());
    }

    @Test
    void shouldKeepTheOldListWhenItsProviderFailsAndStillRefreshTheOthers() {
        // Si borráramos antes de descargar, una caída del sitio de la OFAC dejaría el screening sin lista.
        when(ofacProvider.fetchEntries()).thenThrow(new IllegalStateException("treasury.gov no responde"));
        List<RestrictiveListEntry> onu = List.of(entry(ComplianceListType.ONU, "SANCIONADO UNO"));
        when(onuProvider.fetchEntries()).thenReturn(onu);

        Map<ComplianceListType, RefreshRestrictiveListsUseCase.RefreshResult> results = refreshUseCase.execute();

        verify(repository, never()).deleteByListType(ComplianceListType.OFAC);
        verify(repository).saveAll(onu);
        assertFalse(results.get(ComplianceListType.OFAC).success());
        assertEquals("treasury.gov no responde", results.get(ComplianceListType.OFAC).errorMessage());
        assertTrue(results.get(ComplianceListType.ONU).success());
    }

    @Test
    void shouldAuditManualRefreshWithTheOutcomeOfEachList() {
        when(ofacProvider.fetchEntries()).thenThrow(new IllegalStateException("timeout"));
        when(onuProvider.fetchEntries()).thenReturn(List.of(entry(ComplianceListType.ONU, "SANCIONADO UNO")));
        UUID adminId = UUID.randomUUID();

        new TriggerRestrictiveListRefreshUseCase(refreshUseCase, auditLog).execute(adminId, "181.50.10.2");

        ArgumentCaptor<AdminAuditEntry> entry = ArgumentCaptor.forClass(AdminAuditEntry.class);
        verify(auditLog).log(entry.capture());
        assertEquals("RESTRICTIVE_LIST_REFRESH_TRIGGERED", entry.getValue().getAccion());
        assertEquals(adminId, entry.getValue().getActorAdminId());
        assertEquals("{\"OFAC\":{\"success\":false,\"entries\":0},\"ONU\":{\"success\":true,\"entries\":1}}",
                entry.getValue().getValoresNuevos());
    }

    private RestrictiveListEntry entry(ComplianceListType listType, String name) {
        return RestrictiveListEntry.builder().id(UUID.randomUUID()).listType(listType).fullName(name).normalizedName(name).build();
    }
}
