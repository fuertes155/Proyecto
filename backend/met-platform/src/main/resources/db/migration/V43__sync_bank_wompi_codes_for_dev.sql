-- El catálogo de bancos (V30) no trae wompi_pse_code ni wompi_bank_id — los llena
-- SyncBankCatalogUseCase desde la API real de Wompi. Sin esa sincronización, el
-- depósito PSE nativo, los retiros a banco externo y la verificación de cuenta
-- bancaria fallan con BANK_NOT_SYNCED.
--
-- Para poder probar esos flujos en local (perfil dev usa adaptadores mock que
-- ignoran estos valores) se rellenan con el propio código del banco como
-- placeholder. En producción, SyncBankCatalogUseCase los sobreescribe con los
-- códigos reales de Wompi.
UPDATE banks
   SET wompi_pse_code = COALESCE(NULLIF(wompi_pse_code, ''), code),
       wompi_bank_id  = COALESCE(NULLIF(wompi_bank_id, ''), code)
 WHERE active = true;
