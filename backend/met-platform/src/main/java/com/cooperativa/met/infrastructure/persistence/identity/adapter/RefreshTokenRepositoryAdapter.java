package com.cooperativa.met.infrastructure.persistence.identity.adapter;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.cooperativa.met.domain.identity.model.RefreshToken;
import com.cooperativa.met.domain.identity.port.RefreshTokenRepositoryPort;
import com.cooperativa.met.infrastructure.persistence.identity.entity.RefreshTokenJpaEntity;
import com.cooperativa.met.infrastructure.persistence.identity.repository.RefreshTokenJpaRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepositoryPort {

    private final RefreshTokenJpaRepository repository;

    @Override
    public RefreshToken save(RefreshToken token) {
        RefreshTokenJpaEntity entity = RefreshTokenJpaEntity.fromDomain(token);
        return repository.save(entity).toDomain();
    }

    @Override
    public Optional<RefreshToken> findByJti(UUID jti) {
        return repository.findById(jti).map(RefreshTokenJpaEntity::toDomain);
    }

    @Override
    public void revoke(UUID jti) {
        repository.findById(jti).ifPresent(entity -> {
            entity.setRevoked(true);
            repository.save(entity);
        });
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void revokeAllByUserId(UUID userId) {
        repository.revokeAllByUserId(userId);
    }
}
