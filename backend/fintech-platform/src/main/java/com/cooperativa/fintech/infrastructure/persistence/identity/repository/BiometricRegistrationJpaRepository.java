package com.cooperativa.fintech.infrastructure.persistence.identity.repository;

import com.cooperativa.fintech.infrastructure.persistence.identity.entity.BiometricRegistrationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BiometricRegistrationJpaRepository extends JpaRepository<BiometricRegistrationJpaEntity, UUID> {

    Optional<BiometricRegistrationJpaEntity> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);
}
