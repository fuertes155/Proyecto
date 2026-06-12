package com.cooperativa.fintech.application.compliance.usecase;

import com.cooperativa.fintech.application.compliance.dto.RegulatoryReportResponse;
import com.cooperativa.fintech.application.compliance.mapper.RegulatoryReportMapper;
import com.cooperativa.fintech.domain.common.exception.ResourceNotFoundException;
import com.cooperativa.fintech.domain.compliance.port.RegulatoryReportPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetRegulatoryReportUseCase {

    private final RegulatoryReportPort reportPort;
    private final RegulatoryReportMapper mapper;

    @Transactional(readOnly = true)
    public RegulatoryReportResponse execute(UUID reportId) {
        return reportPort.findById(reportId)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Reporte no encontrado"));
    }
}
