-- ==============================================================================
-- UNIFIED SEED SCRIPT (Desarrollo Local)
-- Carga un administrador y un usuario de prueba en la base de datos `met`.
-- ==============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 1. Admin  (login: admin / admin123)
-- La contraseña debe tener >= 8 caracteres (AdminLoginRequest la valida), por eso
-- 'admin123' y no 'admin'. status DEBE ser 'ACTIVE' (enum AdminStatus = ACTIVE|SUSPENDED);
-- la migración V21 lo insertaba como 'ACTIVO' y rompía el login con 500.
-- DO UPDATE (no DO NOTHING) para reparar filas ya creadas por V21 con hash/status malos.
INSERT INTO admins (username, password_hash, full_name, email, role, status)
VALUES (
    'admin',
    crypt('admin123', gen_salt('bf', 12)),
    'Administrador Principal',
    'admin@met.coop',
    'SUPER_ADMIN',
    'ACTIVE'
)
ON CONFLICT (username) DO UPDATE
    SET password_hash = EXCLUDED.password_hash,
        status        = 'ACTIVE';

-- 2. User  (login: documento 123456 / PIN 1234)
-- Varias columnas van cifradas en reposo con AES_KEY='AES_32_BYTES_KEY_FOR_DEV_LOCAL!!'
-- (la clave de dev que siembra setup_vault_dev.sh / .bat). Los valores de abajo YA están
-- cifrados con esa clave; si cambias AES_KEY hay que regenerarlos:
--
--   * document_number -> AesEncryptionAdapter#encryptDeterministic (AES/CBC + HMAC, IV
--     derivado del texto plano) vía SearchableCryptoConverter. Determinista para poder
--     buscar por igualdad en el WHERE del login. Cifrado de '123456'.
--   * first_name / last_name -> AesEncryptionAdapter#encrypt (AES/GCM, IV aleatorio) vía
--     StandardCryptoConverter. Cifrados de 'Usuario' y 'Test'.
--   * email y pin_hash NO van cifrados (pin_hash es bcrypt).
--
-- Un valor en texto plano en cualquiera de esas columnas revienta al leer la fila
-- (BufferUnderflowException al intentar descifrar) y el login devuelve 500.
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
    'sEjUb8t9NnX7/wz4iTa++1cwR1zU98xX/D2Xy5owDNiu1/eoz0vw3yr2l3gVHSB1OUHHgPHXCepY9kq+r/DnKg==',
    'user@test.com',
    'vM30R+WEkS8ZfyxRdhua/ehtdtkdmEsDiDHTFewqLIs4C8Y=',
    'k6WGKoc0nDO82E+XyLnMBG+yDweKTe6DjV2VzSEOZlY=',
    crypt('1234', gen_salt('bf', 12)),
    'ACTIVE',
    'APPROVED',
    true,
    NOW()
)
ON CONFLICT (document_type, document_number) DO NOTHING;

-- 3. Billetera (core_accounts) del usuario de prueba.
-- En el flujo real la cuenta se crea al terminar el registro biométrico
-- (RegisterBiometricUseCase). El usuario sembrado ya está KYC APPROVED pero sin
-- cuenta, y sin ella el home (/v1/account/summary) y la elegibilidad de crédito
-- (/v1/loans/eligibility -> 422 NO_ACCOUNT) fallan. Le damos un saldo de prueba.
INSERT INTO core_accounts (
    id, user_id, account_number, principal_balance, interest_balance, status, created_at, updated_at
)
SELECT gen_random_uuid(), u.id, '1000000001', 500000.00, 0.00, 'ACTIVE', NOW(), NOW()
FROM users u
WHERE u.email = 'user@test.com'
  AND NOT EXISTS (SELECT 1 FROM core_accounts c WHERE c.user_id = u.id);

-- 4. Catálogo de bancos: habilitar PSE + payout y rellenar los códigos Wompi
-- (con el propio code como placeholder) para que en dev funcionen el depósito
-- PSE nativo, los retiros a banco externo y la verificación de cuenta bancaria.
UPDATE banks
   SET supports_pse = true,
       supports_payout = true,
       wompi_pse_code = COALESCE(NULLIF(wompi_pse_code, ''), code),
       wompi_bank_id  = COALESCE(NULLIF(wompi_bank_id, ''), code)
 WHERE active = true;

-- 5. Dar "ganancias" (interest_balance) al usuario de prueba: los retiros a banco
-- externo solo permiten sacar ganancias, no el capital invertido. Sin esto la
-- pantalla de retiro muestra siempre $0 disponible.
UPDATE core_accounts SET interest_balance = 25000.00
 WHERE user_id IN (SELECT id FROM users WHERE email IN ('user@test.com', 'sam33mol4@gmail.com'));
