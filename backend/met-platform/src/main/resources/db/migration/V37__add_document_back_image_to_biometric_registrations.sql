-- KYC requiere ambos lados de la cédula, no solo el frente.
ALTER TABLE biometric_registrations ADD COLUMN document_back_image TEXT;
