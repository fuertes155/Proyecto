package com.cooperativa.met.infrastructure.persistence.bank.repository;

import com.cooperativa.met.infrastructure.persistence.bank.entity.BankJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BankJpaRepository extends JpaRepository<BankJpaEntity, String> {
    List<BankJpaEntity> findByActiveTrue();
}
