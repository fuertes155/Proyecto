package com.cooperativa.fintech.infrastructure.persistence.compliance.repository;

import com.cooperativa.fintech.domain.compliance.model.SupersolidariaReportType;
import com.cooperativa.fintech.infrastructure.persistence.compliance.entity.RegulatoryReportJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RegulatoryReportJpaRepository extends JpaRepository<RegulatoryReportJpaEntity, UUID> {

    Optional<RegulatoryReportJpaEntity> findByReportTypeAndPeriodYearAndPeriodMonth(
            SupersolidariaReportType reportType, int periodYear, int periodMonth);

    List<RegulatoryReportJpaEntity> findByPeriodYearAndPeriodMonthOrderByReportTypeAsc(
            int periodYear, int periodMonth);

    List<RegulatoryReportJpaEntity> findAllByOrderByCreatedAtDesc();
}
