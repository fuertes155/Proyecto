CREATE EXTENSION IF NOT EXISTS pgcrypto;
INSERT INTO admins (username, password_hash, full_name, email, role, status)
VALUES ('admin', crypt('admin', gen_salt('bf', 12)), 'Administrador Principal', 'admin@met.coop', 'SUPER_ADMIN', 'ACTIVE')
ON CONFLICT (username) DO NOTHING;
