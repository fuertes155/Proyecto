package com.cooperativa.met.infrastructure.persistence.compliance.repository;

import com.cooperativa.met.domain.identity.model.ComplianceListType;
import com.cooperativa.met.infrastructure.persistence.compliance.entity.RestrictiveListEntryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface RestrictiveListEntryJpaRepository extends JpaRepository<RestrictiveListEntryJpaEntity, UUID> {

    long countByListType(ComplianceListType listType);

    @Modifying
    @Transactional
    void deleteByListType(ComplianceListType listType);

    /**
     * Similitud por trigramas (pg_trgm). similarity() devuelve 0.0-1.0; el
     * índice GIN sobre normalized_name hace esto eficiente aun con miles de filas.
     * list_type se compara como texto (columna VARCHAR mapeada por @Enumerated STRING).
     */
    @Query(value = """
            SELECT e.id, e.full_name, e.normalized_name, e.source_ref, e.source_updated_at, e.created_at,
                   similarity(e.normalized_name, :query) AS sim
            FROM restrictive_list_entries e
            WHERE e.list_type = :listType
              AND similarity(e.normalized_name, :query) >= :threshold
            ORDER BY sim DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> searchBySimilarity(
            @Param("query") String normalizedQuery,
            @Param("listType") String listType,
            @Param("threshold") double threshold,
            @Param("limit") int limit);
}
