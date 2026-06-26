package com.cooperativa.met.infrastructure.persistence.investment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "investment_instruments")
@Getter
@Setter
public class InvestmentInstrumentJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "tasa_anual", nullable = false, precision = 8, scale = 6)
    private BigDecimal tasaAnual;

    @Column(name = "plazo_dias", nullable = false)
    private int plazoDias;

    @Column(name = "monto_minimo", nullable = false, precision = 18, scale = 2)
    private BigDecimal montoMinimo;

    @Column(name = "cupo_maximo", precision = 18, scale = 2)
    private BigDecimal cupoMaximo;

    @Column(nullable = false)
    private boolean activo;

    @Column(name = "creado_por")
    private UUID creadoPor;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
