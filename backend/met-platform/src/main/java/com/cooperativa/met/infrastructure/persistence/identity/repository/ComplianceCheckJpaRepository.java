package com.cooperativa.met.infrastructure.persistence.identity.repository;

import com.cooperativa.met.domain.identity.model.ComplianceResult;
import com.cooperativa.met.infrastructure.persistence.identity.entity.ComplianceCheckJpaEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ComplianceCheckJpaRepository extends JpaRepository<ComplianceCheckJpaEntity, UUID> {

    List<ComplianceCheckJpaEntity> findByResult(ComplianceResult result, org.springframework.data.domain.Pageable pageable);

    default List<ComplianceCheckJpaEntity> findByResultPaged(ComplianceResult result, int page, int pageSize) {
        return findByResult(result, PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "checkedAt")));
    }
}
