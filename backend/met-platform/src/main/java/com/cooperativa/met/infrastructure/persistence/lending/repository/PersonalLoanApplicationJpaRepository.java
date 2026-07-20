package com.cooperativa.met.infrastructure.persistence.lending.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.cooperativa.met.domain.lending.model.LoanApplicationStatus;
import com.cooperativa.met.infrastructure.persistence.lending.entity.PersonalLoanApplicationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PersonalLoanApplicationJpaRepository extends JpaRepository<PersonalLoanApplicationJpaEntity, UUID> {

    List<PersonalLoanApplicationJpaEntity> findByUserIdOrderBySubmittedAtDesc(UUID userId);

    List<PersonalLoanApplicationJpaEntity> findByStatusOrderBySubmittedAtAsc(LoanApplicationStatus status);

    Optional<PersonalLoanApplicationJpaEntity> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByUserIdAndStatusIn(UUID userId, List<LoanApplicationStatus> statuses);
}
