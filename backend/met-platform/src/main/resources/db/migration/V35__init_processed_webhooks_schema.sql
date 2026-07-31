-- Registro de idempotencia para webhooks de pago (Wompi real y el gateway simulado de dev).
-- El código (ProcessWebhookUseCase) ya dependía de esta tabla; faltaba la migración.
CREATE TABLE processed_webhooks (
    transaction_id VARCHAR(255) PRIMARY KEY,
    gateway VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL
);
