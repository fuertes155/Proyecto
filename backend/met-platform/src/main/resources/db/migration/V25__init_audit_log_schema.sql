-- ============================================================
-- V25: Tabla de auditoría de operaciones sensibles
-- Registra automáticamente transferencias, cambios de PIN,
-- accesos admin, y cualquier operación crítica para trazabilidad.
-- ============================================================

CREATE TABLE IF NOT EXISTS audit_log (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID,                                  -- NULL si la acción es anónima (ej. login fallido)
    action          VARCHAR(80)  NOT NULL,                 -- ej. TRANSFER_EXECUTED, PIN_CHANGED, LOGIN_FAILED
    entity_type     VARCHAR(60),                           -- ej. TRANSFER, USER, LOAN
    entity_id       VARCHAR(100),                          -- ID de la entidad afectada
    ip_address      VARCHAR(45),                           -- IPv4 o IPv6
    details         TEXT,                                  -- JSON con contexto adicional (monto, destino, etc.)
    result          VARCHAR(20)  NOT NULL DEFAULT 'SUCCESS', -- SUCCESS | FAILURE
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Índices para consultas frecuentes por usuario y por fecha
CREATE INDEX IF NOT EXISTS idx_audit_log_user_id   ON audit_log(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_action     ON audit_log(action);
CREATE INDEX IF NOT EXISTS idx_audit_log_created_at ON audit_log(created_at DESC);

COMMENT ON TABLE audit_log IS 'Registro inmutable de operaciones sensibles para trazabilidad y cumplimiento regulatorio';
