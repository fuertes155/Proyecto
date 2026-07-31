package com.cooperativa.met.infrastructure.persistence.compliance.adapter;

import com.cooperativa.met.domain.compliance.model.RestrictiveListEntry;
import com.cooperativa.met.domain.compliance.model.RestrictiveListMatch;
import com.cooperativa.met.domain.compliance.port.RestrictiveListRepositoryPort;
import com.cooperativa.met.domain.identity.model.ComplianceListType;
import com.cooperativa.met.infrastructure.persistence.compliance.entity.RestrictiveListEntryJpaEntity;
import com.cooperativa.met.infrastructure.persistence.compliance.repository.RestrictiveListEntryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RestrictiveListRepositoryAdapter implements RestrictiveListRepositoryPort {

    private final RestrictiveListEntryJpaRepository repository;

    @Override
    public List<RestrictiveListEntry> saveAll(List<RestrictiveListEntry> entries) {
        List<RestrictiveListEntryJpaEntity> saved = repository.saveAll(entries.stream().map(this::toEntity).toList());
        return saved.stream().map(this::toModel).toList();
    }

    @Override
    public void deleteByListType(ComplianceListType listType) {
        repository.deleteByListType(listType);
    }

    @Override
    public long countByListType(ComplianceListType listType) {
        return repository.countByListType(listType);
    }

    @Override
    public List<RestrictiveListMatch> searchByName(String normalizedName, ComplianceListType listType, double threshold, int limit) {
        List<Object[]> rows = repository.searchBySimilarity(normalizedName, listType.name(), threshold, limit);
        return rows.stream().map(row -> {
            RestrictiveListEntry entry = RestrictiveListEntry.builder()
                    .id((UUID) row[0])
                    .listType(listType)
                    .fullName((String) row[1])
                    .normalizedName((String) row[2])
                    .sourceRef((String) row[3])
                    .sourceUpdatedAt(toInstant(row[4]))
                    .createdAt(toInstant(row[5]))
                    .build();
            double similarity = ((Number) row[6]).doubleValue();
            return new RestrictiveListMatch(entry, similarity);
        }).toList();
    }

    private Instant toInstant(Object value) {
        if (value == null) return null;
        if (value instanceof Instant instant) return instant;
        if (value instanceof OffsetDateTime odt) return odt.toInstant();
        if (value instanceof java.sql.Timestamp ts) return ts.toInstant();
        throw new IllegalStateException("Tipo de fecha no soportado: " + value.getClass());
    }

    private RestrictiveListEntryJpaEntity toEntity(RestrictiveListEntry model) {
        RestrictiveListEntryJpaEntity entity = new RestrictiveListEntryJpaEntity();
        entity.setId(model.getId() != null ? model.getId() : UUID.randomUUID());
        entity.setListType(model.getListType());
        entity.setFullName(model.getFullName());
        entity.setNormalizedName(model.getNormalizedName());
        entity.setSourceRef(model.getSourceRef());
        entity.setSourceUpdatedAt(model.getSourceUpdatedAt());
        entity.setCreatedAt(model.getCreatedAt() != null ? model.getCreatedAt() : Instant.now());
        return entity;
    }

    private RestrictiveListEntry toModel(RestrictiveListEntryJpaEntity entity) {
        return RestrictiveListEntry.builder()
                .id(entity.getId())
                .listType(entity.getListType())
                .fullName(entity.getFullName())
                .normalizedName(entity.getNormalizedName())
                .sourceRef(entity.getSourceRef())
                .sourceUpdatedAt(entity.getSourceUpdatedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
