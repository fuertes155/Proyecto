package com.cooperativa.fintech.domain.lending.port;

import com.cooperativa.fintech.domain.lending.model.AmortizationInstallment;

import java.util.List;
import java.util.UUID;

public interface AmortizationSchedulePort {

    List<AmortizationInstallment> saveAll(UUID applicationId, List<AmortizationInstallment> installments);

    List<AmortizationInstallment> findByApplicationId(UUID applicationId);
}
