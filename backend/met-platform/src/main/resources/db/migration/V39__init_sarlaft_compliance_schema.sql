-- SARLAFT: listas restrictivas reales (OFAC/ONU) + alertas de operaciones inusuales.
-- pg_trgm habilita búsqueda difusa de nombres (coincidencias parciales/errores de
-- tipeo/variantes de acentos), imprescindible para screening de listas de sanciones
-- donde el nombre nunca llega exactamente igual al de la fuente oficial.
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE restrictive_list_entries (
    id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    list_type         VARCHAR(20)  NOT NULL,             -- OFAC | ONU
    full_name         TEXT         NOT NULL,              -- tal como viene de la fuente
    normalized_name   TEXT         NOT NULL,              -- mayúsculas, sin acentos, espacios colapsados
    source_ref        VARCHAR(100),                        -- id/entrada de la fuente (ej. ent_num de OFAC)
    source_updated_at TIMESTAMPTZ,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_restrictive_list_type ON restrictive_list_entries (list_type);
-- Índice GIN trigram: permite similarity()/% con buen rendimiento aun con miles de entradas.
CREATE INDEX idx_restrictive_list_name_trgm ON restrictive_list_entries USING GIN (normalized_name gin_trgm_ops);

CREATE TABLE compliance_alerts (
    id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id           UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    transaction_id    UUID,
    alert_type        VARCHAR(30)  NOT NULL,   -- UNUSUAL_AMOUNT | UNUSUAL_FREQUENCY | STRUCTURING_PATTERN | RAPID_IN_OUT
    severity          VARCHAR(10)  NOT NULL,   -- LOW | MEDIUM | HIGH
    description       TEXT         NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'OPEN', -- OPEN | UNDER_REVIEW | DISMISSED | REPORTED
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    reviewed_by_admin_id UUID REFERENCES admins (id),
    reviewed_at       TIMESTAMPTZ,
    resolution_notes  TEXT
);

CREATE INDEX idx_compliance_alerts_user ON compliance_alerts (user_id);
CREATE INDEX idx_compliance_alerts_status ON compliance_alerts (status, created_at DESC);
