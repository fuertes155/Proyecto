@echo off
REM =========================================================================
REM Script de Configuracion Local de HashiCorp Vault para Met Platform (Windows)
REM =========================================================================

echo Esperando a que Vault inicie...
timeout /t 5 /nobreak >nul

echo Habilitando el motor de secretos KV v2...
docker exec -e VAULT_ADDR=http://127.0.0.1:8200 -e VAULT_TOKEN=root-local-2026 met-vault vault secrets enable -path=secret kv-v2

echo Insertando secretos de desarrollo para met-platform...

docker exec -e VAULT_ADDR=http://127.0.0.1:8200 -e VAULT_TOKEN=root-local-2026 met-vault vault kv put secret/met-platform ^
  DB_PASSWORD="met" ^
  REDIS_PASSWORD="redis-dev-pass" ^
  JWT_SECRET="SUPER_SECRET_JWT_KEY_FOR_DEV_MIN_256_BITS_!@#$" ^
  AES_KEY="AES_32_BYTES_KEY_FOR_DEV_LOCAL!!" ^
  WOMPI_PUBLIC_KEY="pub_test_wompi_dev" ^
  WOMPI_PRIVATE_KEY="prv_test_wompi_dev" ^
  WOMPI_INTEGRITY_SECRET="int_test_wompi_dev" ^
  WOMPI_WEBHOOK_SECRET="wh_test_wompi_dev" ^
  NOTIFICATION_API_KEY="notification_dev_key" ^
  DATACREDITO_CLIENT_SECRET="mock_datacredito_secret" ^
  GEMINI_API_KEY="dev_gemini_key" ^
  MAIL_PASSWORD="dev_mail_password"

echo !Secretos insertados exitosamente en Vault!
echo Ahora Spring Boot los cargara automaticamente durante el arranque.
