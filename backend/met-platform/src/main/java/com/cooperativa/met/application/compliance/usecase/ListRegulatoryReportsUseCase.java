package com.cooperativa.met.application.compliance.usecase;

import com.cooperativa.met.application.compliance.dto.RegulatoryReportResponse;
import com.cooperativa.met.application.compliance.mapper.RegulatoryReportMapper;
import com.cooperativa.met.domain.compliance.port.RegulatoryReportPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListRegulatoryReportsUseCase {

    private final RegulatoryReportPort reportPort;
    private final RegulatoryReportMapper mapper;

    @Transactional(readOnly = true)
    public List<RegulatoryReportResponse> execute(Integer year, Integer month) {
        var reports = (year != null && month != null)
                ? reportPort.findByPeriod(year, month)
                : reportPort.findAll();
        return reports.stream().map(mapper::toResponse).toList();
    }
}
