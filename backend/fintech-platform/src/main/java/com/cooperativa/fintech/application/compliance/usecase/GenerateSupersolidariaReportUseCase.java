package com.cooperativa.fintech.application.compliance.usecase;

import com.cooperativa.fintech.application.compliance.dto.GenerateReportRequest;
import com.cooperativa.fintech.application.compliance.dto.RegulatoryReportResponse;
import com.cooperativa.fintech.application.compliance.mapper.RegulatoryReportMapper;
import com.cooperativa.fintech.domain.common.exception.BusinessRuleException;
import com.cooperativa.fintech.domain.compliance.model.RegulatoryReport;
import com.cooperativa.fintech.domain.compliance.model.ReportDataset;
import com.cooperativa.fintech.domain.compliance.model.ReportStatus;
import com.cooperativa.fintech.domain.compliance.port.RegulatoryReportPort;
import com.cooperativa.fintech.domain.compliance.port.ReportDatasetPort;
import com.cooperativa.fintech.domain.compliance.port.ReportFileStoragePort;
import com.cooperativa.fintech.domain.compliance.service.SupersolidariaFlatFileGenerator;
import com.cooperativa.fintech.infrastructure.config.FintechComplianceProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GenerateSupersolidariaReportUseCase {

    private final RegulatoryReportPort reportPort;
    private final ReportDatasetPort datasetPort;
    private final ReportFileStoragePort storagePort;
    private final FintechComplianceProperties complianceProperties;
    private final RegulatoryReportMapper mapper;

    @Transactional
    public RegulatoryReportResponse execute(UUID userId, GenerateReportRequest request) {
        // userId puede ser null en generación automática programada
        var existing = reportPort.findByTypeAndPeriod(
                request.reportType(), request.periodYear(), request.periodMonth());

        if (existing.isPresent() && existing.get().getStatus() == ReportStatus.COMPLETED) {
            throw new BusinessRuleException("REPORT_ALREADY_EXISTS",
                    "El reporte para este período ya fue generado");
        }

        RegulatoryReport report = existing.orElseGet(() -> reportPort.save(
                RegulatoryReport.builder()
                        .id(UUID.randomUUID())
                        .reportType(request.reportType())
                        .periodYear(request.periodYear())
                        .periodMonth(request.periodMonth())
                        .entityCode(complianceProperties.getEntityCode())
                        .status(ReportStatus.PENDING)
                        .recordCount(0)
                        .generatedBy(userId)
                        .createdAt(Instant.now())
                        .build()
        ));

        report = reportPort.save(report.markGenerating());

        try {
            ReportDataset dataset = datasetPort.buildDataset(
                    request.reportType(),
                    request.periodYear(),
                    request.periodMonth(),
                    complianceProperties.getEntityCode()
            );

            byte[] content = SupersolidariaFlatFileGenerator.generate(dataset);
            String fileName = SupersolidariaFlatFileGenerator.buildFileName(dataset);
            String filePath = storagePort.store(fileName, content);
            String checksum = sha256(content);

            RegulatoryReport completed = reportPort.save(report.markCompleted(
                    fileName, filePath, content.length,
                    dataset.getDetailRows().size(), checksum
            ));

            log.info("Reporte Supersolidaria generado: {} ({})", fileName, completed.getId());
            return mapper.toResponse(completed);

        } catch (Exception ex) {
            log.error("Error generando reporte Supersolidaria", ex);
            RegulatoryReport failed = reportPort.save(report.markFailed(ex.getMessage()));
            throw new BusinessRuleException("REPORT_GENERATION_FAILED",
                    "Error al generar reporte: " + ex.getMessage());
        }
    }

    private String sha256(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content));
        } catch (Exception ex) {
            throw new IllegalStateException("Error calculando checksum", ex);
        }
    }
}
