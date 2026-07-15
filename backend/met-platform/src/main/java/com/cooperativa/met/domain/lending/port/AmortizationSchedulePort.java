package com.cooperativa.met.domain.lending.port;

import com.cooperativa.met.domain.lending.model.AmortizationInstallment;

import java.util.List;
import java.util.UUID;

public interface AmortizationSchedulePort {

    List<AmortizationInstallment> saveAll(UUID applicationId, List<AmortizationInstallment> installments);

    List<AmortizationInstallment> findByApplicationId(UUID applicationId);

    List<AmortizationInstallment> findPendingInstallmentsByDueDateBeforeOrEqual(java.time.LocalDate date);
}
