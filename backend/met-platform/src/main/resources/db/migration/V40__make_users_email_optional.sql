-- El registro simplificado solo pide cédula, nombre completo y PIN (ver RegisterUserUseCase):
-- el correo ya no se captura al crear la cuenta, el usuario lo agrega después desde su perfil.
-- uk_users_email se conserva: en PostgreSQL una restricción UNIQUE admite varios NULL, así que
-- sigue garantizando que no se repitan los correos que sí se registran.
ALTER TABLE users ALTER COLUMN email DROP NOT NULL;
