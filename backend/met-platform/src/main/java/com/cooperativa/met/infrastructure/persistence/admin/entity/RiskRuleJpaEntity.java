package com.cooperativa.met.infrastructure.persistence.admin.entity;

import com.cooperativa.met.domain.admin.model.RiskAction;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "risk_rules")
@Getter
@Setter
public class RiskRuleJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 4000)
    private String descripcion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String condicion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RiskAction accion;

    @Column(nullable = false)
    private boolean activo;

    @Column(name = "creado_por")
    private UUID creadoPor;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
