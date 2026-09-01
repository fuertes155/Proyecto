-- Repara el superadmin creado por V21__insert_superadmin.sql en instalaciones donde:
--   a) se insertó con status 'ACTIVO' (Spanish) — el enum AdminStatus solo tiene
--      ACTIVE | SUSPENDED, así que el login del admin fallaba con 500
--      ("No enum constant ... AdminStatus.ACTIVO").
--   b) el placeholder ${superadmin_password_hash} llegó vacío o corrupto porque
--      SUPERADMIN_PASSWORD_HASH no estaba disponible / los '$' del hash BCrypt se
--      comieron en la interpolación de docker compose — quedando un hash inválido
--      con el que passwordEncoder.matches() nunca hace match.
--
-- En un despliegue real, SUPERADMIN_PASSWORD_HASH se configura bien y estas
-- condiciones no se cumplen, por lo que la migración no toca nada.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

UPDATE admins
   SET status = 'ACTIVE'
 WHERE status = 'ACTIVO';

-- Un hash BCrypt válido son 60 chars y empieza por $2a$/$2b$/$2y$. Si no lo es,
-- se pone uno de desarrollo para la contraseña 'admin123' (>= 8 chars, que es lo
-- que exige AdminLoginRequest).
UPDATE admins
   SET password_hash = crypt('admin123', gen_salt('bf', 12))
 WHERE username = 'admin'
   AND (
        password_hash IS NULL
     OR length(password_hash) <> 60
     OR password_hash NOT LIKE '$2%$%'
   );
