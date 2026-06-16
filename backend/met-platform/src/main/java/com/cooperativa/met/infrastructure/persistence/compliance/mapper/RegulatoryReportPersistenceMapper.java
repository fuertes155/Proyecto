package com.cooperativa.met.infrastructure.persistence.compliance.mapper;

import com.cooperativa.met.domain.compliance.model.RegulatoryReport;
import com.cooperativa.met.infrastructure.persistence.compliance.entity.RegulatoryReportJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class RegulatoryReportPersistenceMapper {

    public RegulatoryReport toDomain(RegulatoryReportJpaEntity e) {
        return RegulatoryReport.builder()
                .id(e.getId()).reportType(e.getReportType())
                .periodYear(e.getPeriodYear()).periodMonth(e.getPeriodMonth())
                .entityCode(e.getEntityCode()).status(e.getStatus())
                .fileName(e.getFileName()).filePath(e.getFilePath())
                .fileSizeBytes(e.getFileSizeBytes()).recordCount(e.getRecordCount())
                .checksumSha256(e.getChecksumSha256()).generatedBy(e.getGeneratedBy())
                .errorMessage(e.getErrorMessage()).generatedAt(e.getGeneratedAt())
                .createdAt(e.getCreatedAt())
                .build();
    }

    public RegulatoryReportJpaEntity toEntity(RegulatoryReport r) {
        RegulatoryReportJpaEntity e = new RegulatoryReportJpaEntity();
        e.setId(r.getId()); e.setReportType(r.getReportType());
        e.setPeriodYear(r.getPeriodYear()); e.setPeriodMonth(r.getPeriodMonth());
        e.setEntityCode(r.getEntityCode()); e.setStatus(r.getStatus());
        e.setFileName(r.getFileName()); e.setFilePath(r.getFilePath());
        e.setFileSizeBytes(r.getFileSizeBytes()); e.setRecordCount(r.getRecordCount());
        e.setChecksumSha256(r.getChecksumSha256()); e.setGeneratedBy(r.getGeneratedBy());
        e.setErrorMessage(r.getErrorMessage()); e.setGeneratedAt(r.getGeneratedAt());
        e.setCreatedAt(r.getCreatedAt());
        return e;
    }
}
