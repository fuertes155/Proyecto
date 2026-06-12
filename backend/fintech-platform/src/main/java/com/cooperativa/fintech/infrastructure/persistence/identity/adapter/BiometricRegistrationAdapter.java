package com.cooperativa.fintech.infrastructure.persistence.identity.adapter;

import com.cooperativa.fintech.domain.identity.model.BiometricRegistration;
import com.cooperativa.fintech.domain.identity.port.BiometricRegistrationPort;
import com.cooperativa.fintech.infrastructure.persistence.identity.mapper.BiometricPersistenceMapper;
import com.cooperativa.fintech.infrastructure.persistence.identity.repository.BiometricRegistrationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BiometricRegistrationAdapter implements BiometricRegistrationPort {

    private final BiometricRegistrationJpaRepository repository;
    private final BiometricPersistenceMapper mapper;

    @Override
    public BiometricRegistration save(BiometricRegistration registration) {
        return mapper.toDomain(repository.save(mapper.toEntity(registration)));
    }

    @Override
    public Optional<BiometricRegistration> findLatestByUserId(UUID userId) {
        return repository.findFirstByUserIdOrderByCreatedAtDesc(userId).map(mapper::toDomain);
    }
}
