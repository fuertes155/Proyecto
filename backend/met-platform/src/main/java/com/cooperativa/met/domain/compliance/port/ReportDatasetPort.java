package com.cooperativa.met.domain.compliance.port;

import com.cooperativa.met.domain.compliance.model.ReportDataset;
import com.cooperativa.met.domain.compliance.model.SupersolidariaReportType;

public interface ReportDatasetPort {

    ReportDataset buildDataset(SupersolidariaReportType type, int year, int month, String entityCode);
}
