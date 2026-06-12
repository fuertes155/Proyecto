package com.cooperativa.fintech.domain.compliance.port;

import com.cooperativa.fintech.domain.compliance.model.ReportDataset;
import com.cooperativa.fintech.domain.compliance.model.SupersolidariaReportType;

public interface ReportDatasetPort {

    ReportDataset buildDataset(SupersolidariaReportType type, int year, int month, String entityCode);
}
