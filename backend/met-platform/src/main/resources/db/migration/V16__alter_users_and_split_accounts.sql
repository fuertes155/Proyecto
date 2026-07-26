-- V16: Fusión de dos cambios estructurales aplicados en el mismo sprint
-- 1) Ampliar columnas de usuarios para soportar encriptación AES (Base64)
ALTER TABLE users ALTER COLUMN document_number TYPE VARCHAR(255);
ALTER TABLE users ALTER COLUMN phone TYPE VARCHAR(255);
ALTER TABLE users ALTER COLUMN first_name TYPE VARCHAR(255);
ALTER TABLE users ALTER COLUMN last_name TYPE VARCHAR(255);

-- 2) Separar el balance de la cuenta principal en dos columnas
ALTER TABLE core_accounts RENAME COLUMN balance TO principal_balance;
ALTER TABLE core_accounts ADD COLUMN interest_balance DECIMAL(15, 2) NOT NULL DEFAULT 50.00;
