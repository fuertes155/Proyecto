package com.cooperativa.met.infrastructure.persistence.lending.repository;

import com.cooperativa.met.infrastructure.persistence.lending.entity.PersonalLoanAmortizationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PersonalLoanAmortizationJpaRepository extends JpaRepository<PersonalLoanAmortizationJpaEntity, UUID> {

    List<PersonalLoanAmortizationJpaEntity> findByApplicationIdOrderByInstallmentNumberAsc(UUID applicationId);

    @org.springframework.data.jpa.repository.Query("SELECT a FROM PersonalLoanAmortizationJpaEntity a WHERE a.status = 'PENDING' AND a.dueDate <= :currentDate")
    List<PersonalLoanAmortizationJpaEntity> findPendingInstallmentsByDueDateBeforeOrEqual(@org.springframework.data.repository.query.Param("currentDate") java.time.LocalDate currentDate);
    
    @org.springframework.data.jpa.repository.Query("SELECT a FROM PersonalLoanAmortizationJpaEntity a WHERE a.status = 'LATE'")
    List<PersonalLoanAmortizationJpaEntity> findAllLateInstallments();
}
