package com.cooperativa.met.infrastructure.persistence.solidarity.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.cooperativa.met.domain.solidarity.model.MicroLoanStatus;
import com.cooperativa.met.infrastructure.persistence.solidarity.entity.MicroLoanJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MicroLoanJpaRepository extends JpaRepository<MicroLoanJpaEntity, UUID> {

    List<MicroLoanJpaEntity> findByGroupIdOrderByRequestedAtDesc(UUID groupId);

    List<MicroLoanJpaEntity> findByGroupIdAndStatusOrderByRequestedAtDesc(UUID groupId, MicroLoanStatus status);

    boolean existsByGroupIdAndBorrowerIdAndStatusIn(
            UUID groupId, UUID borrowerId, List<MicroLoanStatus> statuses);
}
