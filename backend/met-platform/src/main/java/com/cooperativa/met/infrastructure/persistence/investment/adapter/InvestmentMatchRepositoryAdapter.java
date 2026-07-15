package com.cooperativa.met.infrastructure.persistence.investment.adapter;

import com.cooperativa.met.domain.investment.model.InvestmentMatch;
import com.cooperativa.met.domain.investment.port.InvestmentMatchRepositoryPort;
import com.cooperativa.met.infrastructure.persistence.investment.entity.InvestmentMatchJpaEntity;
import com.cooperativa.met.infrastructure.persistence.investment.repository.InvestmentMatchJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class InvestmentMatchRepositoryAdapter implements InvestmentMatchRepositoryPort {

    private final InvestmentMatchJpaRepository repository;

    @Override
    public InvestmentMatch save(InvestmentMatch match) {
        return toModel(repository.save(toEntity(match)));
    }

    @Override
    public List<InvestmentMatch> saveAll(List<InvestmentMatch> matches) {
        List<InvestmentMatchJpaEntity> entities = matches.stream().map(this::toEntity).toList();
        return repository.saveAll(entities).stream().map(this::toModel).toList();
    }

    @Override
    public Optional<InvestmentMatch> findById(UUID id) {
        return repository.findById(id).map(this::toModel);
    }

    @Override
    public List<InvestmentMatch> findByBorrowerLoanId(UUID loanId) {
        return repository.findByBorrowerLoanId(loanId).stream().map(this::toModel).toList();
    }

    @Override
    public List<InvestmentMatch> findByFractionId(UUID fractionId) {
        return repository.findByFractionId(fractionId).stream().map(this::toModel).toList();
    }

    private InvestmentMatchJpaEntity toEntity(InvestmentMatch model) {
        return InvestmentMatchJpaEntity.builder()
                .id(model.getId())
                .fractionId(model.getFractionId())
                .borrowerLoanId(model.getBorrowerLoanId())
                .matchedAt(model.getMatchedAt())
                .build();
    }

    private InvestmentMatch toModel(InvestmentMatchJpaEntity entity) {
        return InvestmentMatch.builder()
                .id(entity.getId())
                .fractionId(entity.getFractionId())
                .borrowerLoanId(entity.getBorrowerLoanId())
                .matchedAt(entity.getMatchedAt())
                .build();
    }
}
