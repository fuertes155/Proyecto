package com.cooperativa.met.infrastructure.persistence.solidarity.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.cooperativa.met.infrastructure.persistence.solidarity.entity.PoolTransactionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PoolTransactionJpaRepository extends JpaRepository<PoolTransactionJpaEntity, UUID> {

    List<PoolTransactionJpaEntity> findByGroupIdOrderByCreatedAtDesc(UUID groupId);
}
