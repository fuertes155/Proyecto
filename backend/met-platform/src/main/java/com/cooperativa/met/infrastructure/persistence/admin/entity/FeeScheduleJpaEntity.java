package com.cooperativa.met.infrastructure.persistence.admin.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fee_schedule")
@Getter
@Setter
public class FeeScheduleJpaEntity {

    @Id
    private UUID id;

    @Column(name = "tipo_tarifa", nullable = false, length = 50)
    private String tipoTarifa;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal valor;

    @Column(name = "es_porcentaje", nullable = false)
    private boolean esPorcentaje;

    @Column(name = "vigente_desde", nullable = false)
    private Instant vigentDesde;

    @Column(name = "vigente_hasta")
    private Instant vigentaHasta;

    @Column(name = "creado_por")
    private UUID creadoPor;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
