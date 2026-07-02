CREATE EXTENSION IF NOT EXISTS pgcrypto;
INSERT INTO users (
    document_type,
    document_number,
    email,
    first_name,
    last_name,
    pin_hash,
    status,
    kyc_status,
    terms_accepted,
    terms_accepted_at
)
VALUES (
    'CC',
    '123456',
    'user@test.com',
    'Usuario',
    'Test',
    crypt('1234', gen_salt('bf', 12)),
    'ACTIVE',
    'APPROVED',
    true,
    NOW()
)
ON CONFLICT (document_type, document_number) DO NOTHING;
