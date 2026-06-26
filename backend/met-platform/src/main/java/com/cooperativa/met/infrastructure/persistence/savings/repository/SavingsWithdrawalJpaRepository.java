package com.cooperativa.met.infrastructure.persistence.savings.repository;

import com.cooperativa.met.infrastructure.persistence.savings.entity.SavingsWithdrawalJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SavingsWithdrawalJpaRepository extends JpaRepository<SavingsWithdrawalJpaEntity, UUID> {
}
