package com.cooperativa.met.infrastructure.persistence.account.repository;

import com.cooperativa.met.infrastructure.persistence.account.entity.CoreAccountJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CoreAccountJpaRepository extends JpaRepository<CoreAccountJpaEntity, UUID> {
    Optional<CoreAccountJpaEntity> findByUserId(UUID userId);
    Optional<CoreAccountJpaEntity> findByAccountNumber(String accountNumber);
}
