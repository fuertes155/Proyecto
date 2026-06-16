package com.cooperativa.met.domain.compliance.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ReportDataRow {

    private final List<String> fields;

    public String toFlatLine(String delimiter) {
        return String.join(delimiter, fields);
    }
}
