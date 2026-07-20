package com.cooperativa.met.infrastructure.persistence.solidarity.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.cooperativa.met.domain.solidarity.model.InstallmentStatus;
import com.cooperativa.met.infrastructure.persistence.solidarity.entity.LoanInstallmentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LoanInstallmentJpaRepository extends JpaRepository<LoanInstallmentJpaEntity, UUID> {

    List<LoanInstallmentJpaEntity> findByLoanIdOrderByInstallmentNumberAsc(UUID loanId);

    long countByLoanIdAndStatusNot(UUID loanId, InstallmentStatus status);
}
