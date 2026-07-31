package com.cooperativa.met.domain.compliance.model;

import com.cooperativa.met.domain.identity.model.ComplianceListType;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * Una entrada individual de una lista restrictiva (OFAC/ONU) ya descargada e
 * indexada localmente. El screening nunca llama al proveedor externo en
 * caliente — siempre consulta esta tabla, que se refresca periódicamente.
 */
@Getter
@Builder(toBuilder = true)
public class RestrictiveListEntry {

    private final UUID id;
    private final ComplianceListType listType;
    private final String fullName;
    private final String normalizedName;
    private final String sourceRef;
    private final Instant sourceUpdatedAt;
    private final Instant createdAt;
}
