package com.cooperativa.met.infrastructure.persistence.compliance.entity;

import com.cooperativa.met.domain.identity.model.ComplianceListType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "restrictive_list_entries")
@Getter
@Setter
public class RestrictiveListEntryJpaEntity {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "list_type", nullable = false, length = 20)
    private ComplianceListType listType;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "normalized_name", nullable = false)
    private String normalizedName;

    @Column(name = "source_ref", length = 100)
    private String sourceRef;

    @Column(name = "source_updated_at")
    private Instant sourceUpdatedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
