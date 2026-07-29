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
-- document_number va cifrado (AES-CBC determinista, ver SearchableCryptoConverter /
-- AesEncryptionAdapter#encryptDeterministic) porque la app siempre cifra el valor antes de
-- comparar en el WHERE; un valor en texto plano aquí nunca haría match con la búsqueda por
-- documento (login, recuperación de PIN, etc.). El valor de abajo es el cifrado de '123456'
-- con la clave de desarrollo AES_KEY='AES_32_BYTES_KEY_FOR_DEV_LOCAL!!' (ver setup_vault_dev.sh).
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
    '4QrcOUm6Wau+VuBX8g+IPhjK+oF3SZmIy71U/tNhaP4=',
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
