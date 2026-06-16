package com.cooperativa.met.infrastructure.persistence.lending.adapter;

import com.cooperativa.met.domain.lending.model.AmortizationInstallment;
import com.cooperativa.met.domain.lending.port.AmortizationSchedulePort;
import com.cooperativa.met.infrastructure.persistence.lending.entity.PersonalLoanAmortizationJpaEntity;
import com.cooperativa.met.infrastructure.persistence.lending.mapper.LendingPersistenceMapper;
import com.cooperativa.met.infrastructure.persistence.lending.repository.PersonalLoanAmortizationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AmortizationScheduleAdapter implements AmortizationSchedulePort {

    private final PersonalLoanAmortizationJpaRepository repository;
    private final LendingPersistenceMapper mapper;

    @Override
    public List<AmortizationInstallment> saveAll(UUID applicationId, List<AmortizationInstallment> installments) {
        List<PersonalLoanAmortizationJpaEntity> entities = installments.stream()
                .map(i -> {
                    AmortizationInstallment withAppId = AmortizationInstallment.builder()
                            .id(UUID.randomUUID())
                            .applicationId(applicationId)
                            .installmentNumber(i.getInstallmentNumber())
                            .paymentAmount(i.getPaymentAmount())
                            .principalAmount(i.getPrincipalAmount())
                            .interestAmount(i.getInterestAmount())
                            .remainingBalance(i.getRemainingBalance())
                            .dueDate(i.getDueDate())
                            .build();
                    return mapper.toEntity(withAppId);
                })
                .toList();
        return repository.saveAll(entities).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<AmortizationInstallment> findByApplicationId(UUID applicationId) {
        return repository.findByApplicationIdOrderByInstallmentNumberAsc(applicationId).stream()
                .map(mapper::toDomain).toList();
    }
}
