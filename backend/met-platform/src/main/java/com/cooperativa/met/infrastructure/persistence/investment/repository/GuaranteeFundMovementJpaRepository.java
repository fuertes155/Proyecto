package com.cooperativa.met.infrastructure.persistence.investment.repository;

import com.cooperativa.met.infrastructure.persistence.investment.entity.GuaranteeFundMovementJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface GuaranteeFundMovementJpaRepository extends JpaRepository<GuaranteeFundMovementJpaEntity, UUID> {

    List<GuaranteeFundMovementJpaEntity> findByTransactionReference(UUID transactionReference);

    @Query("SELECT COALESCE(SUM(CASE WHEN m.type = 'CONTRIBUTION' THEN m.amount ELSE -m.amount END), 0) " +
            "FROM GuaranteeFundMovementJpaEntity m")
    BigDecimal calculateBalance();
}
