-- Fracciones de inversión (cada fragmento del depósito)
CREATE TABLE investment_fractions (
    id UUID PRIMARY KEY,
    investor_account_id UUID NOT NULL REFERENCES core_accounts(id),
    original_deposit_id UUID NOT NULL,          -- transacción de depósito origen
    amount DECIMAL(15,2) NOT NULL,              -- monto de la fracción (ej: $10.000)
    status VARCHAR(20) NOT NULL,                -- AVAILABLE | MATCHED | RETURNED
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    matched_at TIMESTAMP WITH TIME ZONE
);

-- Tabla pivote: qué fracciones están asignadas a qué deudor
CREATE TABLE investment_matches (
    id UUID PRIMARY KEY,
    fraction_id UUID NOT NULL REFERENCES investment_fractions(id),
    borrower_loan_id UUID NOT NULL REFERENCES personal_loan_applications(id),
    matched_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Libro contable de doble entrada
CREATE TABLE ledger_entries (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES core_accounts(id),
    transaction_reference UUID NOT NULL,        -- ID de la fracción o match
    entry_type VARCHAR(10) NOT NULL,            -- DEBIT | CREDIT
    amount DECIMAL(15,2) NOT NULL,
    concept VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
