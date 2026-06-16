package com.cooperativa.met.infrastructure.persistence.lending.adapter;

import com.cooperativa.met.domain.lending.model.LoanApplicationStatus;
import com.cooperativa.met.domain.lending.model.PersonalLoanApplication;
import com.cooperativa.met.domain.lending.port.PersonalLoanApplicationPort;
import com.cooperativa.met.infrastructure.persistence.lending.mapper.LendingPersistenceMapper;
import com.cooperativa.met.infrastructure.persistence.lending.repository.PersonalLoanApplicationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PersonalLoanApplicationAdapter implements PersonalLoanApplicationPort {

    private static final List<LoanApplicationStatus> PENDING_STATUSES = List.of(
            LoanApplicationStatus.SUBMITTED,
            LoanApplicationStatus.IN_REVIEW,
            LoanApplicationStatus.APPROVED
    );

    private final PersonalLoanApplicationJpaRepository repository;
    private final LendingPersistenceMapper mapper;

    @Override
    public PersonalLoanApplication save(PersonalLoanApplication application) {
        return mapper.toDomain(repository.save(mapper.toEntity(application)));
    }

    @Override
    public Optional<PersonalLoanApplication> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<PersonalLoanApplication> findByIdAndUserId(UUID id, UUID userId) {
        return repository.findByIdAndUserId(id, userId).map(mapper::toDomain);
    }

    @Override
    public List<PersonalLoanApplication> findByUserId(UUID userId) {
        return repository.findByUserIdOrderBySubmittedAtDesc(userId).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public boolean hasPendingApplication(UUID userId) {
        return repository.existsByUserIdAndStatusIn(userId, PENDING_STATUSES);
    }
}
