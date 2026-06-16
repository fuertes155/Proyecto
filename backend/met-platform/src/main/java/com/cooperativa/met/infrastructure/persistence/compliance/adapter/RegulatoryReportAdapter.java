package com.cooperativa.met.infrastructure.persistence.compliance.adapter;

import com.cooperativa.met.domain.compliance.model.RegulatoryReport;
import com.cooperativa.met.domain.compliance.model.SupersolidariaReportType;
import com.cooperativa.met.domain.compliance.port.RegulatoryReportPort;
import com.cooperativa.met.infrastructure.persistence.compliance.mapper.RegulatoryReportPersistenceMapper;
import com.cooperativa.met.infrastructure.persistence.compliance.repository.RegulatoryReportJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RegulatoryReportAdapter implements RegulatoryReportPort {

    private final RegulatoryReportJpaRepository repository;
    private final RegulatoryReportPersistenceMapper mapper;

    @Override
    public RegulatoryReport save(RegulatoryReport report) {
        return mapper.toDomain(repository.save(mapper.toEntity(report)));
    }

    @Override
    public Optional<RegulatoryReport> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<RegulatoryReport> findByTypeAndPeriod(SupersolidariaReportType type, int year, int month) {
        return repository.findByReportTypeAndPeriodYearAndPeriodMonth(type, year, month).map(mapper::toDomain);
    }

    @Override
    public List<RegulatoryReport> findAll() {
        return repository.findAllByOrderByCreatedAtDesc().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<RegulatoryReport> findByPeriod(int year, int month) {
        return repository.findByPeriodYearAndPeriodMonthOrderByReportTypeAsc(year, month).stream()
                .map(mapper::toDomain).toList();
    }
}
