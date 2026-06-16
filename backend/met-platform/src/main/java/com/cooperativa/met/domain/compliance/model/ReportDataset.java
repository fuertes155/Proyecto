package com.cooperativa.met.domain.compliance.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ReportDataset {

    private final SupersolidariaReportType reportType;
    private final int periodYear;
    private final int periodMonth;
    private final String entityCode;
    private final List<String> headerFields;
    private final List<ReportDataRow> detailRows;
}
