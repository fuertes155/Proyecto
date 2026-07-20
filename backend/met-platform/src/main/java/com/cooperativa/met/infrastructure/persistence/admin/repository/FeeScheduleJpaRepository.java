package com.cooperativa.met.infrastructure.persistence.admin.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.cooperativa.met.infrastructure.persistence.admin.entity.FeeScheduleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface FeeScheduleJpaRepository extends JpaRepository<FeeScheduleJpaEntity, UUID> {

    @Query("SELECT f FROM FeeScheduleJpaEntity f WHERE f.vigentaHasta IS NULL OR f.vigentaHasta > :now")
    List<FeeScheduleJpaEntity> findVigentes(@Param("now") Instant now);

    @Modifying
    @Query("UPDATE FeeScheduleJpaEntity f SET f.vigentaHasta = :ahora WHERE f.tipoTarifa = :tipo AND f.vigentaHasta IS NULL")
    void cerrarVigencia(@Param("tipo") String tipoTarifa, @Param("ahora") Instant ahora);
}
