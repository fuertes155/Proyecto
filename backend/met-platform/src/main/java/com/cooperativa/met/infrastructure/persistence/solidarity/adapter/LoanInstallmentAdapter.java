package com.cooperativa.met.infrastructure.persistence.solidarity.adapter;

import com.cooperativa.met.domain.solidarity.model.InstallmentStatus;
import com.cooperativa.met.domain.solidarity.model.LoanInstallment;
import com.cooperativa.met.domain.solidarity.port.LoanInstallmentPort;
import com.cooperativa.met.infrastructure.persistence.solidarity.mapper.SolidarityPersistenceMapper;
import com.cooperativa.met.infrastructure.persistence.solidarity.repository.LoanInstallmentJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LoanInstallmentAdapter implements LoanInstallmentPort {

    private final LoanInstallmentJpaRepository repository;
    private final SolidarityPersistenceMapper mapper;

    @Override
    public List<LoanInstallment> saveAll(List<LoanInstallment> installments) {
        return repository.saveAll(installments.stream().map(mapper::toEntity).toList())
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public LoanInstallment save(LoanInstallment installment) {
        return mapper.toDomain(repository.save(mapper.toEntity(installment)));
    }

    @Override
    public List<LoanInstallment> findByLoanId(UUID loanId) {
        return repository.findByLoanIdOrderByInstallmentNumberAsc(loanId).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public Optional<LoanInstallment> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public boolean allPaid(UUID loanId) {
        return repository.countByLoanIdAndStatusNot(loanId, InstallmentStatus.PAID) == 0;
    }
}
