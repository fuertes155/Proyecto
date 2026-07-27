package com.cooperativa.met.infrastructure.persistence.admin.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "maintenance_windows")
@Getter
@Setter
public class MaintenanceWindowJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 4000)
    private String descripcion;

    @Column(nullable = false)
    private Instant inicio;

    @Column(nullable = false)
    private Instant fin;

    @Column(nullable = false)
    private boolean activo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "endpoints_activos")
    private String endpointsActivos;

    @Column(name = "creado_por")
    private UUID creadoPor;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
