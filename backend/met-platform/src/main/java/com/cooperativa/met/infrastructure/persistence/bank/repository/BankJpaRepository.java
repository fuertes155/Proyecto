package com.cooperativa.met.infrastructure.persistence.bank.repository;

import com.cooperativa.met.infrastructure.persistence.bank.entity.BankJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BankJpaRepository extends JpaRepository<BankJpaEntity, String> {
    List<BankJpaEntity> findByActiveTrue();
}
