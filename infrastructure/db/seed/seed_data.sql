-- ==============================================================================
-- UNIFIED SEED SCRIPT (Desarrollo Local)
-- Carga un administrador y un usuario de prueba en la base de datos `met`.
-- ==============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 1. Admin
INSERT INTO admins (username, password_hash, full_name, email, role, status)
VALUES (
    'admin', 
    crypt('admin', gen_salt('bf', 12)), 
    'Administrador Principal', 
    'admin@met.coop', 
    'SUPER_ADMIN', 
    'ACTIVE'
)
ON CONFLICT (username) DO NOTHING;

-- 2. User
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
