package com.cooperativa.fintech.infrastructure.persistence.lending.repository;

import com.cooperativa.fintech.infrastructure.persistence.lending.entity.PersonalLoanAmortizationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PersonalLoanAmortizationJpaRepository extends JpaRepository<PersonalLoanAmortizationJpaEntity, UUID> {

    List<PersonalLoanAmortizationJpaEntity> findByApplicationIdOrderByInstallmentNumberAsc(UUID applicationId);
}
