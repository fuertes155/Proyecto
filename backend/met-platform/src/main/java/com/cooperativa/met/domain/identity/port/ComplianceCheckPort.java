package com.cooperativa.met.domain.identity.port;

import com.cooperativa.met.domain.identity.model.ComplianceCheckRecord;
import com.cooperativa.met.domain.identity.model.ComplianceListType;
import com.cooperativa.met.domain.identity.model.ComplianceResult;

import java.util.List;
import java.util.UUID;

public interface ComplianceCheckPort {

    ComplianceResult checkUser(UUID userId, ComplianceListType listType);

    void persistCheck(UUID userId, ComplianceListType listType, ComplianceResult result, String details);

    List<ComplianceCheckRecord> findByResult(ComplianceResult result, int page, int pageSize);
}
