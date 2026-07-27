package com.cooperativa.met.infrastructure.persistence.admin.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "admin_audit_log")
@Getter
@Setter
public class AdminAuditLogJpaEntity {

    @Id
    private UUID id;

    @Column(name = "actor_admin_id", nullable = false)
    private UUID actorAdminId;

    @Column(nullable = false, length = 100)
    private String accion;

    @Column(name = "entidad_afectada", length = 100)
    private String entidadAfectada;

    @Column(name = "id_entidad", length = 255)
    private String idEntidad;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "valores_anteriores")
    private String valoresAnteriores;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "valores_nuevos")
    private String valoresNuevos;

    @Column(length = 4000)
    private String motivo;

    @Column(name = "ip_origen", length = 45)
    private String ipOrigen;

    @Column(nullable = false)
    private Instant timestamp;
}
