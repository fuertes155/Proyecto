package com.cooperativa.met.infrastructure.persistence.identity.adapter;

import com.cooperativa.met.domain.compliance.model.RestrictiveListMatch;
import com.cooperativa.met.domain.compliance.port.RestrictiveListRepositoryPort;
import com.cooperativa.met.domain.compliance.service.NameNormalizer;
import com.cooperativa.met.domain.identity.model.ComplianceListType;
import com.cooperativa.met.domain.identity.model.ComplianceResult;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.port.ComplianceCheckPort;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import com.cooperativa.met.application.compliance.usecase.RefreshRestrictiveListsUseCase;
import com.cooperativa.met.infrastructure.config.MetComplianceProperties;
import com.cooperativa.met.infrastructure.persistence.identity.entity.ComplianceCheckJpaEntity;
import com.cooperativa.met.infrastructure.persistence.identity.repository.ComplianceCheckJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Screening real contra listas restrictivas (OFAC/ONU), por nombre normalizado
 * y similitud de trigramas contra {@code restrictive_list_entries} (ver
 * {@link RefreshRestrictiveListsUseCase} para cómo se llena esa tabla — nunca
 * se llama al proveedor externo en caliente, solo se consulta el caché local).
 *
 * SARLAFT no es una lista externa descargable (es el nombre del régimen), así
 * que para ese {@link ComplianceListType} no hay nada que screenear todavía —
 * se deja en CLEAR de forma explícita hasta que exista una fuente real que
 * cubra (ej. listas locales de la Superintendencia, PEP, etc.).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ComplianceCheckAdapter implements ComplianceCheckPort {

    private final ComplianceCheckJpaRepository repository;
    private final RestrictiveListRepositoryPort restrictiveListRepository;
    private final UserRepositoryPort userRepository;
    private final MetComplianceProperties complianceProperties;

    @Override
    public ComplianceResult checkUser(UUID userId, ComplianceListType listType) {
        if (listType == ComplianceListType.SARLAFT) {
            return ComplianceResult.CLEAR;
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("Screening {} solicitado para userId={} pero el usuario no existe.", listType, userId);
            return ComplianceResult.ERROR;
        }

        String fullName = (user.getFirstName() + " " + user.getLastName()).trim();
        String normalized = NameNormalizer.normalize(fullName);
        if (normalized.isBlank()) {
            return ComplianceResult.ERROR;
        }

        double threshold = complianceProperties.getSarlaft().getRestrictiveListMatchThreshold();
        try {
            List<RestrictiveListMatch> matches = restrictiveListRepository.searchByName(normalized, listType, threshold, 5);
            if (!matches.isEmpty()) {
                RestrictiveListMatch best = matches.get(0);
                log.warn("Posible coincidencia en lista {} para userId={}: \"{}\" ~ \"{}\" (similitud={})",
                        listType, userId, fullName, best.entry().getFullName(), best.similarity());
                return ComplianceResult.MATCH;
            }
            return ComplianceResult.CLEAR;
        } catch (Exception e) {
            // Si la tabla local aún no tiene datos (nunca corrió el refresco) o falla la
            // consulta, no se debe bloquear el registro/solicitud de crédito de todo el
            // mundo por un problema de infraestructura — se reporta como ERROR (no CLEAR,
            // para que quede evidencia de que este usuario no fue realmente screeneado).
            log.error("Error consultando la lista restrictiva {} para userId={}", listType, userId, e);
            return ComplianceResult.ERROR;
        }
    }

    @Override
    // REQUIRES_NEW: el llamador (KYC, solicitud de crédito) suele lanzar una excepción
    // justo después de un MATCH para abortar su propio flujo — si este insert viviera en
    // esa misma transacción, se revertiría junto con ella y el MATCH (el caso que más
    // importa auditar) nunca quedaría registrado en compliance_checks.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistCheck(UUID userId, ComplianceListType listType, ComplianceResult result, String details) {
        ComplianceCheckJpaEntity entity = new ComplianceCheckJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setUserId(userId);
        entity.setListType(listType);
        entity.setResult(result);
        entity.setCheckedAt(Instant.now());
        entity.setDetails(details);
        repository.save(entity);
    }

    @Override
    public List<com.cooperativa.met.domain.identity.model.ComplianceCheckRecord> findByResult(ComplianceResult result, int page, int pageSize) {
        return repository.findByResultPaged(result, page, pageSize).stream()
                .map(e -> com.cooperativa.met.domain.identity.model.ComplianceCheckRecord.builder()
                        .id(e.getId())
                        .userId(e.getUserId())
                        .listType(e.getListType())
                        .result(e.getResult())
                        .checkedAt(e.getCheckedAt())
                        .details(e.getDetails())
                        .build())
                .toList();
    }
}
