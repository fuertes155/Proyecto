-- =====================================================================
-- V9: Actualizar monto de regla de riesgo "Monto alto transferencia"
--     De $10.000.000 → $5.000.000 (solicitado en revisión 02/07/2026)
-- =====================================================================

UPDATE risk_rules
SET
    condicion   = '{"field": "amount", "operator": "gt", "value": 5000000}',
    descripcion = 'Transferencias superiores a $5.000.000 requieren revisión'
WHERE nombre = 'Monto alto transferencia';
