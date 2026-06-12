package com.cooperativa.fintech.domain.compliance.port;

import com.cooperativa.fintech.domain.compliance.model.RegulatoryReport;
import com.cooperativa.fintech.domain.compliance.model.SupersolidariaReportType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RegulatoryReportPort {

    RegulatoryReport save(RegulatoryReport report);

    Optional<RegulatoryReport> findById(UUID id);

    Optional<RegulatoryReport> findByTypeAndPeriod(
            SupersolidariaReportType type, int year, int month);

    List<RegulatoryReport> findAll();

    List<RegulatoryReport> findByPeriod(int year, int month);
}
