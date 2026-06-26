package com.cooperativa.met.infrastructure.persistence.investment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "micro_investments")
@Getter
@Setter
public class MicroInvestmentJpaEntity {

    @Id
    private UUID id;

    @Column(name = "portfolio_id", nullable = false)
    private UUID portfolioId;

    @Column(name = "instrument_id", nullable = false)
    private UUID instrumentId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "monto_invertido", nullable = false, precision = 18, scale = 2)
    private BigDecimal montoInvertido;

    @Column(name = "tasa_aplicada", nullable = false, precision = 8, scale = 6)
    private BigDecimal tasaAplicada;

    @Column(name = "plazo_dias", nullable = false)
    private int plazoDias;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Column(name = "rendimiento_ganado", nullable = false, precision = 18, scale = 2)
    private BigDecimal rendimientoGanado;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(name = "cancelado_at")
    private Instant canceladoAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
