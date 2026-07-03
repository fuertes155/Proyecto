-- =====================================================================
-- V8: Admin Module Schema
-- Tablas: admins, admin_audit_log, operation_limits,
--         fee_schedule, risk_rules, maintenance_windows
-- =====================================================================

-- Administradores del sistema (separados de usuarios clientes)
CREATE TABLE admins (
    id            UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    username      VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(200) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    role          VARCHAR(30)  NOT NULL DEFAULT 'ADMIN',
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_admins_username UNIQUE (username),
    CONSTRAINT uk_admins_email    UNIQUE (email)
);

CREATE INDEX idx_admins_status ON admins (status);
CREATE INDEX idx_admins_role   ON admins (role);

-- -----------------------------------------------------------------------
-- Auditoría de acciones administrativas (append-only)
-- -----------------------------------------------------------------------
CREATE TABLE admin_audit_log (
    id                UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    actor_admin_id    UUID         NOT NULL REFERENCES admins(id),
    accion            VARCHAR(100) NOT NULL,
    entidad_afectada  VARCHAR(100),
    id_entidad        VARCHAR(255),
    valores_anteriores JSONB,
    valores_nuevos    JSONB,
    motivo            TEXT,
    ip_origen         VARCHAR(45),
    timestamp         TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_actor     ON admin_audit_log (actor_admin_id);
CREATE INDEX idx_audit_accion    ON admin_audit_log (accion);
CREATE INDEX idx_audit_entidad   ON admin_audit_log (entidad_afectada, id_entidad);
CREATE INDEX idx_audit_timestamp ON admin_audit_log (timestamp DESC);

-- -----------------------------------------------------------------------
-- Límites de operación diarios por tipo
-- -----------------------------------------------------------------------
CREATE TABLE operation_limits (
    id                          UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    tipo_operacion              VARCHAR(50)  NOT NULL,
    monto_diario_max            BIGINT       NOT NULL,
    monto_por_transaccion_max   BIGINT       NOT NULL,
    activo                      BOOLEAN      NOT NULL DEFAULT true,
    creado_por                  UUID         REFERENCES admins(id),
    updated_at                  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_limits_tipo UNIQUE (tipo_operacion)
);

INSERT INTO operation_limits (tipo_operacion, monto_diario_max, monto_por_transaccion_max)
VALUES
    ('TRANSFER',   500000000, 100000000),
    ('WITHDRAWAL', 200000000,  50000000),
    ('PURCHASE',   300000000,  80000000);

-- -----------------------------------------------------------------------
-- Tarifas con versionado (vigente_desde / vigente_hasta)
-- -----------------------------------------------------------------------
CREATE TABLE fee_schedule (
    id            UUID          PRIMARY KEY DEFAULT uuid_generate_v4(),
    tipo_tarifa   VARCHAR(50)   NOT NULL,
    descripcion   TEXT,
    valor         NUMERIC(10,4) NOT NULL,
    es_porcentaje BOOLEAN       NOT NULL DEFAULT true,
    vigente_desde TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    vigente_hasta TIMESTAMPTZ,
    creado_por    UUID          REFERENCES admins(id),
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_fees_tipo    ON fee_schedule (tipo_tarifa);
CREATE INDEX idx_fees_vigente ON fee_schedule (vigente_desde, vigente_hasta);

INSERT INTO fee_schedule (tipo_tarifa, descripcion, valor, es_porcentaje, vigente_desde)
VALUES
    ('TRANSFER_FEE',  'Comisión por transferencia',    0.0050, true, NOW()),
    ('WITHDRAWAL_FEE','Comisión por retiro',            0.0025, true, NOW()),
    ('LOAN_INTEREST', 'Tasa de interés crédito simple', 0.0180, true, NOW());

-- -----------------------------------------------------------------------
-- Reglas de riesgo automáticas
-- -----------------------------------------------------------------------
CREATE TABLE risk_rules (
    id          UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    nombre      VARCHAR(100) NOT NULL,
    descripcion TEXT,
    condicion   JSONB        NOT NULL,
    accion      VARCHAR(20)  NOT NULL DEFAULT 'REVIEW',
    activo      BOOLEAN      NOT NULL DEFAULT true,
    creado_por  UUID         REFERENCES admins(id),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_risk_accion CHECK (accion IN ('ALLOW', 'BLOCK', 'REVIEW'))
);

CREATE INDEX idx_risk_activo ON risk_rules (activo);

INSERT INTO risk_rules (nombre, descripcion, condicion, accion)
VALUES
    ('Monto alto transferencia',
     'Transferencias superiores a $5.000.000 requieren revisión',
     '{"field": "amount", "operator": "gt", "value": 5000000}',
     'REVIEW'),
    ('Múltiples intentos fallidos',
     'Más de 5 intentos fallidos de PIN en 10 minutos',
     '{"field": "failed_attempts", "operator": "gt", "value": 5, "window_minutes": 10}',
     'BLOCK');

-- -----------------------------------------------------------------------
-- Ventanas de mantenimiento programado
-- -----------------------------------------------------------------------
CREATE TABLE maintenance_windows (
    id                UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    descripcion       TEXT         NOT NULL,
    inicio            TIMESTAMPTZ  NOT NULL,
    fin               TIMESTAMPTZ  NOT NULL,
    activo            BOOLEAN      NOT NULL DEFAULT false,
    endpoints_activos JSONB,
    creado_por        UUID         REFERENCES admins(id),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_maintenance_activo ON maintenance_windows (activo);
CREATE INDEX idx_maintenance_tiempo ON maintenance_windows (inicio, fin);

-- -----------------------------------------------------------------------
-- Refresh tokens para admins (separados de los de usuarios)
-- -----------------------------------------------------------------------
CREATE TABLE admin_refresh_tokens (
    id         UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    jti        UUID         NOT NULL,
    admin_id   UUID         NOT NULL REFERENCES admins(id) ON DELETE CASCADE,
    issued_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ  NOT NULL,
    revoked    BOOLEAN      NOT NULL DEFAULT false,
    CONSTRAINT uk_admin_refresh_jti UNIQUE (jti)
);

CREATE INDEX idx_admin_refresh_admin_id ON admin_refresh_tokens (admin_id);
CREATE INDEX idx_admin_refresh_jti      ON admin_refresh_tokens (jti);
