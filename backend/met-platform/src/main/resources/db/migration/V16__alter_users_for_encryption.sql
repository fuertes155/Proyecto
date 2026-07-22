-- Modificar columnas para soportar encriptacion Base64 (AES)
ALTER TABLE users ALTER COLUMN document_number TYPE VARCHAR(255);
ALTER TABLE users ALTER COLUMN phone TYPE VARCHAR(255);
ALTER TABLE users ALTER COLUMN first_name TYPE VARCHAR(255);
ALTER TABLE users ALTER COLUMN last_name TYPE VARCHAR(255);
