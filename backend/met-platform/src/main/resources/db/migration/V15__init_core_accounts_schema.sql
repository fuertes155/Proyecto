CREATE TABLE core_accounts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    account_number VARCHAR(50) NOT NULL UNIQUE,
    balance DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE core_transactions (
    id UUID PRIMARY KEY,
    source_account_id UUID REFERENCES core_accounts(id),
    destination_account_id UUID REFERENCES core_accounts(id),
    amount DECIMAL(15, 2) NOT NULL,
    concept VARCHAR(255),
    type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Trigger para que todo usuario nuevo tenga automáticamente una cuenta creada
-- Pero como ya existen usuarios, crearemos una cuenta inicial ficticia a los existentes (o lo haremos vía script).
-- Crearemos una cuenta inicial a todos los usuarios actuales con un saldo de regalo de $500 para pruebas.
INSERT INTO core_accounts (id, user_id, account_number, balance, status, created_at, updated_at)
SELECT 
    gen_random_uuid(), 
    id, 
    '10' || lpad(floor(random() * 100000000)::text, 8, '0'), 
    500.00, 
    'ACTIVE', 
    CURRENT_TIMESTAMP, 
    CURRENT_TIMESTAMP
FROM users;
