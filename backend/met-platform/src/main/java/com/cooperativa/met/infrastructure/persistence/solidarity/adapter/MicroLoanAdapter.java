package com.cooperativa.met.infrastructure.persistence.solidarity.adapter;

import com.cooperativa.met.domain.solidarity.model.MicroLoan;
import com.cooperativa.met.domain.solidarity.model.MicroLoanStatus;
import com.cooperativa.met.domain.solidarity.port.MicroLoanPort;
import com.cooperativa.met.infrastructure.persistence.solidarity.mapper.SolidarityPersistenceMapper;
import com.cooperativa.met.infrastructure.persistence.solidarity.repository.MicroLoanJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MicroLoanAdapter implements MicroLoanPort {

    private static final List<MicroLoanStatus> ACTIVE_STATUSES = List.of(
            MicroLoanStatus.PENDING, MicroLoanStatus.APPROVED, MicroLoanStatus.DISBURSED
    );

    private final MicroLoanJpaRepository repository;
    private final SolidarityPersistenceMapper mapper;

    @Override
    public MicroLoan save(MicroLoan loan) {
        return mapper.toDomain(repository.save(mapper.toEntity(loan)));
    }

    @Override
    public Optional<MicroLoan> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<MicroLoan> findByGroupId(UUID groupId) {
        return repository.findByGroupIdOrderByRequestedAtDesc(groupId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean hasActiveLoan(UUID groupId, UUID borrowerId) {
        return repository.existsByGroupIdAndBorrowerIdAndStatusIn(groupId, borrowerId, ACTIVE_STATUSES);
    }

    @Override
    public List<MicroLoan> findByGroupIdAndStatus(UUID groupId, MicroLoanStatus status) {
        return repository.findByGroupIdAndStatusOrderByRequestedAtDesc(groupId, status).stream()
                .map(mapper::toDomain).toList();
    }
}
