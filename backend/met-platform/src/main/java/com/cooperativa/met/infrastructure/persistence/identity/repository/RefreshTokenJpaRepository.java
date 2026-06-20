package com.cooperativa.met.infrastructure.persistence.identity.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cooperativa.met.infrastructure.persistence.identity.entity.RefreshTokenJpaEntity;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenJpaEntity, UUID> {
}
