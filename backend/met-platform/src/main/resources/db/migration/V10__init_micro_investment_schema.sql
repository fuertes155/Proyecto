-- =====================================================================
-- V10: Micro-Investments Module Schema
-- Tablas: investment_instruments, micro_investment_portfolios,
--         micro_investments, investment_returns
-- =====================================================================

-- Catálogo de instrumentos de inversión disponibles (administrable desde el panel admin)
CREATE TABLE investment_instruments (
    id              UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    nombre          VARCHAR(100)    NOT NULL,
    descripcion     TEXT,
    tasa_anual      NUMERIC(8, 6)   NOT NULL,   -- Ej: 0.085000 = 8.5% anual
    plazo_dias      INTEGER         NOT NULL,   -- Duración en días: 30, 60, 90, 180, 365
    monto_minimo    DECIMAL(18, 2)  NOT NULL,   -- Monto mínimo para invertir
    cupo_maximo     DECIMAL(18, 2),             -- Cupo total del instrumento (NULL = ilimitado)
    activo          BOOLEAN         NOT NULL DEFAULT true,
    creado_por      UUID            REFERENCES admins(id),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_tasa_positiva      CHECK (tasa_anual > 0),
    CONSTRAINT chk_plazo_positivo     CHECK (plazo_dias > 0),
    CONSTRAINT chk_monto_min_positivo CHECK (monto_minimo > 0)
);

CREATE INDEX idx_instruments_activo ON investment_instruments (activo);

-- Instrumentos semilla (tasas representativas de cooperativa colombiana)
INSERT INTO investment_instruments (nombre, descripcion, tasa_anual, plazo_dias, monto_minimo)
VALUES
    ('Fondo Liquidez 30D',
     'Inversión de corto plazo con alta liquidez. Plazo 30 días.',
     0.065000, 30, 5000.00),
    ('CDT Cooperativo 90D',
     'Certificado de Depósito a Término a 90 días. Rendimiento moderado.',
     0.082000, 90, 50000.00),
    ('CDT Cooperativo 180D',
     'Certificado de Depósito a Término a 180 días. Mejor rendimiento.',
     0.094000, 180, 100000.00),
    ('CDT Premium 365D',
     'Inversión anual con la mejor tasa disponible para socios.',
     0.110000, 365, 500000.00);

-- -----------------------------------------------------------------------
-- Portfolio de micro-inversiones por usuario
-- Agrupa todas las posiciones creadas en una sola distribución
-- -----------------------------------------------------------------------
CREATE TABLE micro_investment_portfolios (
    id              UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    monto_total     DECIMAL(18, 2)  NOT NULL,
    estrategia      VARCHAR(20)     NOT NULL DEFAULT 'EQUAL',  -- EQUAL, WEIGHTED, RISK_BASED
    estado          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, COMPLETED, CANCELLED
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_monto_total_positivo CHECK (monto_total > 0),
    CONSTRAINT chk_estrategia           CHECK (estrategia IN ('EQUAL', 'WEIGHTED', 'RISK_BASED')),
    CONSTRAINT chk_estado_portfolio     CHECK (estado IN ('ACTIVE', 'COMPLETED', 'CANCELLED'))
);

CREATE INDEX idx_portfolios_user   ON micro_investment_portfolios (user_id);
CREATE INDEX idx_portfolios_estado ON micro_investment_portfolios (estado);

-- -----------------------------------------------------------------------
-- Posición individual de micro-inversión dentro de un portfolio
-- -----------------------------------------------------------------------
CREATE TABLE micro_investments (
    id                  UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    portfolio_id        UUID            NOT NULL REFERENCES micro_investment_portfolios(id) ON DELETE CASCADE,
    instrument_id       UUID            NOT NULL REFERENCES investment_instruments(id),
    user_id             UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    monto_invertido     DECIMAL(18, 2)  NOT NULL,
    tasa_aplicada       NUMERIC(8, 6)   NOT NULL,   -- Snapshot de la tasa al momento de invertir
    plazo_dias          INTEGER         NOT NULL,   -- Snapshot del plazo al momento de invertir
    fecha_inicio        DATE            NOT NULL,
    fecha_vencimiento   DATE            NOT NULL,
    rendimiento_ganado  DECIMAL(18, 2)  NOT NULL DEFAULT 0,
    estado              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, COMPLETED, CANCELLED
    cancelado_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_monto_invertido_pos CHECK (monto_invertido > 0),
    CONSTRAINT chk_tasa_aplicada_pos   CHECK (tasa_aplicada > 0),
    CONSTRAINT chk_estado_inversion    CHECK (estado IN ('ACTIVE', 'COMPLETED', 'CANCELLED'))
);

CREATE INDEX idx_investments_portfolio      ON micro_investments (portfolio_id);
CREATE INDEX idx_investments_user           ON micro_investments (user_id);
CREATE INDEX idx_investments_vencimiento    ON micro_investments (fecha_vencimiento, estado);
CREATE INDEX idx_investments_instrument     ON micro_investments (instrument_id);

-- -----------------------------------------------------------------------
-- Historial de rendimientos pagados al vencer una posición
-- -----------------------------------------------------------------------
CREATE TABLE investment_returns (
    id              UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    investment_id   UUID            NOT NULL REFERENCES micro_investments(id),
    user_id         UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    capital         DECIMAL(18, 2)  NOT NULL,
    rendimiento     DECIMAL(18, 2)  NOT NULL,
    total_acreditado DECIMAL(18, 2) NOT NULL,  -- capital + rendimiento
    fecha_pago      DATE            NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_capital_positivo     CHECK (capital > 0),
    CONSTRAINT chk_rendimiento_pos      CHECK (rendimiento >= 0),
    CONSTRAINT chk_total_acreditado_pos CHECK (total_acreditado > 0)
);

CREATE INDEX idx_returns_investment ON investment_returns (investment_id);
CREATE INDEX idx_returns_user       ON investment_returns (user_id);
CREATE INDEX idx_returns_fecha      ON investment_returns (fecha_pago DESC);

-- Agregar tarifa de inversión al catálogo de fee_schedule
INSERT INTO fee_schedule (tipo_tarifa, descripcion, valor, es_porcentaje, vigente_desde)
VALUES
    ('INVESTMENT_FEE', 'Comisión por creación de portfolio de micro-inversión', 0.0100, true, NOW());
