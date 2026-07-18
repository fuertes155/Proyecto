-- Agregar límites de transferencia por día y por transacción
ALTER TABLE met.core_accounts
ADD COLUMN daily_transfer_limit NUMERIC(19, 2) DEFAULT 5000000.00,
ADD COLUMN per_transaction_limit NUMERIC(19, 2) DEFAULT 2000000.00;
