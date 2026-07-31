-- KYC: firma digital capturada en el registro, debe coincidir con la firma de la cédula.
ALTER TABLE biometric_registrations ADD COLUMN signature_image TEXT;
