-- =====================================================================
-- V30: Catálogo de bancos + cuentas bancarias externas + payouts
-- Habilita el retiro/transferencia a una cuenta bancaria propia del
-- usuario en cualquier banco soportado (Pagos a Terceros de Wompi).
-- =====================================================================

-- -----------------------------------------------------------------------
-- Catálogo de bancos. Solo se siembra el nombre (dato público estable);
-- el código de Wompi (wompi_bank_id) y si soporta PSE/payout se
-- completan mediante SyncBankCatalogUseCase, que consulta la API real
-- de Wompi (GET /banks, GET /pse/financial_institutions) — no se
-- inventan códigos de proveedor en esta migración.
-- -----------------------------------------------------------------------
CREATE TABLE banks (
    code             VARCHAR(20)  PRIMARY KEY,
    name             VARCHAR(100) NOT NULL,
    wompi_bank_id    VARCHAR(20),
    supports_pse     BOOLEAN      NOT NULL DEFAULT false,
    supports_payout  BOOLEAN      NOT NULL DEFAULT false,
    active           BOOLEAN      NOT NULL DEFAULT true,
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

INSERT INTO banks (code, name) VALUES
    ('BANCOLOMBIA',    'Bancolombia'),
    ('DAVIVIENDA',     'Davivienda'),
    ('BBVA_CO',        'BBVA Colombia'),
    ('BANCO_BOGOTA',   'Banco de Bogotá'),
    ('BANCO_OCCIDENTE','Banco de Occidente'),
    ('BANCO_POPULAR',  'Banco Popular'),
    ('CAJA_SOCIAL',    'Banco Caja Social'),
    ('SCOTIABANK_COLPATRIA', 'Scotiabank Colpatria'),
    ('ITAU_CO',        'Itaú Colombia'),
    ('AV_VILLAS',      'Banco AV Villas'),
    ('BANCO_AGRARIO',  'Banco Agrario'),
    ('BANCOOMEVA',     'Bancoomeva'),
    ('NEQUI',          'Nequi'),
    ('DAVIPLATA',      'Daviplata');

-- -----------------------------------------------------------------------
-- Cuentas bancarias externas registradas por el usuario. Solo pueden ser
-- cuentas del propio usuario: el número de identificación del titular
-- que se envía al riel de pago SIEMPRE se deriva de la identidad KYC del
-- usuario autenticado (users.document_type/document_number), nunca de
-- un campo editable en esta tabla — así se garantiza por diseño que un
-- payout jamás pueda dirigirse a la identidad de otra persona.
-- -----------------------------------------------------------------------
CREATE TABLE external_bank_accounts (
    id                   UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id              UUID         NOT NULL REFERENCES users(id),
    bank_code            VARCHAR(20)  NOT NULL REFERENCES banks(code),
    account_type         VARCHAR(20)  NOT NULL,
    account_number       TEXT         NOT NULL, -- cifrado a nivel de aplicación (StandardCryptoConverter)
    verification_status  VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    active               BOOLEAN      NOT NULL DEFAULT true,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    verified_at          TIMESTAMPTZ,
    CONSTRAINT chk_ext_account_type CHECK (account_type IN ('SAVINGS', 'CHECKING')),
    CONSTRAINT chk_ext_verification_status CHECK (verification_status IN ('PENDING', 'VERIFIED', 'REJECTED'))
);

CREATE INDEX idx_ext_bank_accounts_user ON external_bank_accounts (user_id) WHERE active;

-- -----------------------------------------------------------------------
-- Payouts: el core_transaction asociado nace en estado PENDING (el
-- débito ya ocurrió en core_accounts) y pasa a COMPLETED o REVERSED
-- cuando llega la confirmación asíncrona del riel vía webhook.
-- -----------------------------------------------------------------------
CREATE TABLE external_payouts (
    id                       UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    core_transaction_id     UUID         NOT NULL UNIQUE REFERENCES core_transactions(id),
    external_bank_account_id UUID        NOT NULL REFERENCES external_bank_accounts(id),
    rail_reference           VARCHAR(100),
    failure_code             VARCHAR(50),
    failure_message          TEXT,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    settled_at                TIMESTAMPTZ
);

CREATE INDEX idx_ext_payouts_rail_reference ON external_payouts (rail_reference);

-- -----------------------------------------------------------------------
-- Límites de operación para EXTERNAL_PAYOUT (reutiliza operation_limits,
-- hasta ahora sin conectar a ningún caso de uso). Dos escalones:
--   - EXTERNAL_PAYOUT: cuenta bancaria destino ya VERIFIED.
--   - EXTERNAL_PAYOUT_UNVERIFIED: cuenta destino aún PENDING (no existe
--     todavía un mecanismo real de verificación de titularidad — ver
--     RegisterExternalBankAccountUseCase). Techo deliberadamente bajo
--     como mitigación mientras se define esa pieza de negocio.
-- Ambos son MÁS estrictos que el default de transferencia interna
-- ($5.000.000 diario / $2.000.000 por transacción en core_accounts),
-- porque un payout que sale del ledger de la plataforma es más difícil
-- de revertir y de investigar que una transferencia interna.
-- Valores provisionales — deben ser confirmados por el equipo de
-- riesgo/cumplimiento antes de producción (ver PayoutLimitService).
-- -----------------------------------------------------------------------
INSERT INTO operation_limits (tipo_operacion, monto_diario_max, monto_por_transaccion_max)
VALUES
    ('EXTERNAL_PAYOUT', 3000000, 1500000),
    ('EXTERNAL_PAYOUT_UNVERIFIED', 500000, 300000)
ON CONFLICT (tipo_operacion) DO NOTHING;
