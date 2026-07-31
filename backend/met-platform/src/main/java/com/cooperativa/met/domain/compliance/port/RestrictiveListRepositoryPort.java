package com.cooperativa.met.domain.compliance.port;

import com.cooperativa.met.domain.compliance.model.RestrictiveListEntry;
import com.cooperativa.met.domain.compliance.model.RestrictiveListMatch;
import com.cooperativa.met.domain.identity.model.ComplianceListType;

import java.util.List;

public interface RestrictiveListRepositoryPort {

    List<RestrictiveListEntry> saveAll(List<RestrictiveListEntry> entries);

    /** Reemplaza por completo las entradas de una lista (se usa en cada refresco). */
    void deleteByListType(ComplianceListType listType);

    long countByListType(ComplianceListType listType);

    /**
     * Búsqueda difusa por similitud de trigramas (pg_trgm), restringida a una
     * lista concreta. Devuelve las entradas cuya similitud contra
     * {@code normalizedName} sea >= threshold, ordenadas de más a menos parecidas.
     */
    List<RestrictiveListMatch> searchByName(String normalizedName, ComplianceListType listType, double threshold, int limit);
}
