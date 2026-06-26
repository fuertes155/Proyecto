package com.cooperativa.met.infrastructure.persistence.investment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "investment_returns")
@Getter
@Setter
public class InvestmentReturnJpaEntity {

    @Id
    private UUID id;

    @Column(name = "investment_id", nullable = false)
    private UUID investmentId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal capital;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal rendimiento;

    @Column(name = "total_acreditado", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAcreditado;

    @Column(name = "fecha_pago", nullable = false)
    private LocalDate fechaPago;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
