-- Insertar el primer Super Administrador (Contraseña: admin123)
-- El hash BCrypt corresponde a 'admin123'
INSERT INTO admins (id, username, full_name, password_hash, email, role, status, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'admin',
    'Super Administrador',
    '$2a$10$vI8aWBnW3fID.ZQ4/zo1G.q1lRps.9cGLcZEiGDMVr5yUP1KUOYTa',
    'admin@metcooperativa.com',
    'SUPER_ADMIN',
    'ACTIVO',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (username) DO NOTHING;
