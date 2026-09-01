-- V30 sembró el catálogo de bancos con solo (code, name); supports_pse y
-- supports_payout quedaron en su DEFAULT false, así que GET /v1/banks?type=PSE y
-- ?type=PAYOUT devolvían lista vacía y el usuario no podía elegir banco para
-- depósito PSE nativo ni para retiros a cuenta externa.
--
-- Habilita ambos canales para todos los bancos activos del catálogo. En un
-- despliegue real esto se ajusta por banco según los convenios vigentes.
UPDATE banks
   SET supports_pse = true,
       supports_payout = true
 WHERE active = true;
