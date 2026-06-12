package com.cooperativa.fintech.infrastructure.persistence.lending.repository;

import com.cooperativa.fintech.domain.lending.model.LoanApplicationStatus;
import com.cooperativa.fintech.infrastructure.persistence.lending.entity.PersonalLoanApplicationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PersonalLoanApplicationJpaRepository extends JpaRepository<PersonalLoanApplicationJpaEntity, UUID> {

    List<PersonalLoanApplicationJpaEntity> findByUserIdOrderBySubmittedAtDesc(UUID userId);

    Optional<PersonalLoanApplicationJpaEntity> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByUserIdAndStatusIn(UUID userId, List<LoanApplicationStatus> statuses);
}
