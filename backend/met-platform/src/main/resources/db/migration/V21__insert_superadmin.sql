-- Insertar el primer Super Administrador
-- SEGURIDAD: La contraseña se inyecta desde la variable de entorno SUPERADMIN_PASSWORD_HASH
-- en el servidor de producción. Genera un hash BCrypt con:
--   htpasswd -bnBC 12 "" <TU_CONTRASEÑA> | tr -d ':\n' | sed 's/$2y/$2a/'
-- Luego configura: SPRING_FLYWAY_PLACEHOLDERS_SUPERADMIN_PASSWORD_HASH=<el-hash>
INSERT INTO admins (id, username, full_name, password_hash, email, role, status, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'admin',
    'Super Administrador',
    '${superadmin_password_hash}',
    'admin@metcooperativa.com',
    'SUPER_ADMIN',
    'ACTIVO',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (username) DO NOTHING;

