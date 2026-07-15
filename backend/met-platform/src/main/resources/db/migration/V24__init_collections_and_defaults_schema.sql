-- Agregar interés de mora y estado a las cuotas de amortización
ALTER TABLE personal_loan_amortization
ADD COLUMN penalty_interest_amount DECIMAL(18,2) DEFAULT 0.00,
ADD COLUMN status VARCHAR(20) DEFAULT 'PENDING';

-- Agregar token de tarjeta guardado al usuario (simulado) para débitos automáticos
ALTER TABLE users
ADD COLUMN payment_card_token VARCHAR(255) NULL;

-- Asignar un token dummy a algunos usuarios para poder probar el débito recurrente
UPDATE users
SET payment_card_token = 'tok_simulated_wompi_card_998877'
WHERE document_number = '1234567890';
