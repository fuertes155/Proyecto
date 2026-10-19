package com.cooperativa.met.infrastructure.persistence.admin.repository;

import com.cooperativa.met.infrastructure.persistence.admin.entity.PlatformRevenueJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PlatformRevenueJpaRepository extends JpaRepository<PlatformRevenueJpaEntity, UUID> {
}
