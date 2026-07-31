-- Motor de Distribución de Capital: categorización contable + Fondo de Garantías.

-- Categoría del movimiento contable (independiente de DEBIT/CREDIT), para poder
-- auditar cuánto fue capital, rendimiento, comisión de plataforma o fondo.
ALTER TABLE ledger_entries
ADD COLUMN category VARCHAR(40) NOT NULL DEFAULT 'OTHER';

-- Los movimientos de dispersión ya existentes corresponden a fondeo inicial.
UPDATE ledger_entries SET category = 'FUNDING_DISBURSEMENT' WHERE category = 'OTHER';

ALTER TABLE ledger_entries ALTER COLUMN category DROP DEFAULT;

-- Fondo de Garantías/Aval Colectivo: libro de movimientos (el saldo es la suma).
CREATE TABLE guarantee_fund_movements (
    id UUID PRIMARY KEY,
    type VARCHAR(20) NOT NULL,                  -- CONTRIBUTION | PAYOUT
    amount DECIMAL(15,2) NOT NULL,
    transaction_reference UUID NOT NULL,        -- préstamo o desembolso origen del movimiento
    concept VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
