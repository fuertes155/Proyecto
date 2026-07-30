-- Código de institución financiera PSE (producto Checkout/Transacciones de
-- Wompi) es un namespace de IDs DISTINTO al de "Pagos a Terceros" (payout,
-- columna wompi_bank_id) — un mismo banco puede tener códigos diferentes en
-- cada producto de Wompi, así que no deben compartir columna.
ALTER TABLE banks
ADD COLUMN wompi_pse_code VARCHAR(20);
