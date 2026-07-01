-- Agregar columnas para guardar si el usuario aceptó términos y condiciones
ALTER TABLE users 
ADD COLUMN terms_accepted BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN terms_accepted_at TIMESTAMP WITH TIME ZONE;
