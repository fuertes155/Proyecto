-- Verificación de titularidad por micro-depósito: se envía un monto
-- pequeño y aleatorio a la cuenta bancaria externa (fondeado por la cuenta
-- operativa de la plataforma, no por el usuario) y el usuario debe
-- confirmar el monto exacto que vio en su extracto bancario. Cifrado igual
-- que account_number, porque gatea una decisión de confianza financiera.
ALTER TABLE external_bank_accounts
ADD COLUMN pending_verification_amount TEXT,
ADD COLUMN verification_attempts INT NOT NULL DEFAULT 0;
