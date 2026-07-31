package com.cooperativa.met.application.compliance.usecase;

import com.cooperativa.met.domain.compliance.model.RestrictiveListEntry;
import com.cooperativa.met.domain.compliance.port.RestrictiveListProviderPort;
import com.cooperativa.met.domain.compliance.port.RestrictiveListRepositoryPort;
import com.cooperativa.met.domain.identity.model.ComplianceListType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Descarga cada lista restrictiva configurada (OFAC, ONU) y reemplaza por
 * completo lo que hay guardado localmente. Si un proveedor falla (el sitio
 * externo no responde, cambió de formato, etc.) NO se cae todo el refresco —
 * se deja la lista de ese proveedor tal como estaba y se reporta el error,
 * para que el screening con la otra lista siga funcionando.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshRestrictiveListsUseCase {

    private final List<RestrictiveListProviderPort> providers;
    private final RestrictiveListRepositoryPort repository;

    public record RefreshResult(ComplianceListType listType, boolean success, int entriesLoaded, String errorMessage) {
    }

    public RefreshResult refreshOne(RestrictiveListProviderPort provider) {
        ComplianceListType listType = provider.getListType();
        try {
            List<RestrictiveListEntry> entries = provider.fetchEntries();
            repository.deleteByListType(listType);
            repository.saveAll(entries);
            log.info("Lista restrictiva {} refrescada: {} entradas cargadas.", listType, entries.size());
            return new RefreshResult(listType, true, entries.size(), null);
        } catch (Exception e) {
            log.error("Fallo al refrescar la lista restrictiva {}: {}", listType, e.getMessage(), e);
            return new RefreshResult(listType, false, 0, e.getMessage());
        }
    }

    public Map<ComplianceListType, RefreshResult> execute() {
        Map<ComplianceListType, RefreshResult> results = new LinkedHashMap<>();
        for (RestrictiveListProviderPort provider : providers) {
            results.put(provider.getListType(), refreshOne(provider));
        }
        return results;
    }
}
