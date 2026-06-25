package com.cooperativa.met.infrastructure.persistence.admin.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "operation_limits")
@Getter
@Setter
public class OperationLimitJpaEntity {

    @Id
    private UUID id;

    @Column(name = "tipo_operacion", nullable = false, unique = true, length = 50)
    private String tipoOperacion;

    @Column(name = "monto_diario_max", nullable = false)
    private long montoDiarioMax;

    @Column(name = "monto_por_transaccion_max", nullable = false)
    private long montoPorTransaccionMax;

    @Column(nullable = false)
    private boolean activo;

    @Column(name = "creado_por")
    private UUID creadoPor;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
