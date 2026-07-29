@echo off
REM =========================================================================
REM Script de Configuracion Local de HashiCorp Vault para Met Platform (Windows)
REM
REM IMPORTANTE: el contenedor met-vault corre en modo "dev" y guarda los
REM secretos EN MEMORIA. Cada vez que se reinicia (docker compose down/up,
REM reinicio de Docker Desktop, reinicio del PC) los secretos se pierden y el
REM backend falla al arrancar con:
REM   "Invalid met.security.jwt.secret: must be at least 256-bit / 32 bytes"
REM Vuelve a ejecutar este script despues de cada reinicio de Vault.
REM =========================================================================

setlocal

REM El token debe coincidir con VAULT_TOKEN del archivo .env de la raiz.
if "%VAULT_TOKEN%"=="" set "VAULT_TOKEN=root-local-2026"

echo Esperando a que Vault inicie...
timeout /t 5 /nobreak >nul

echo Habilitando el motor de secretos KV v2 (ignora el error si ya existe)...
docker exec -e VAULT_ADDR=http://127.0.0.1:8200 -e VAULT_TOKEN=%VAULT_TOKEN% met-vault vault secrets enable -path=secret kv-v2

echo Insertando secretos de desarrollo para met-platform...

docker exec -e VAULT_ADDR=http://127.0.0.1:8200 -e VAULT_TOKEN=%VAULT_TOKEN% met-vault vault kv put secret/met-platform ^
  DB_PASSWORD="MetDev2026Secure" ^
  REDIS_PASSWORD="redis-dev-pass" ^
  JWT_SECRET="SUPER_SECRET_JWT_KEY_FOR_DEV_MIN_256_BITS_!@#$" ^
  AES_KEY="AES_32_BYTES_KEY_FOR_DEV_LOCAL!!" ^
  HMAC_SECRET="D3vHmacS3cr3tKey123!@#" ^
  SUPERADMIN_PASSWORD_HASH="$2a$12$EOKeWz34L2tNqGOimqGBjeo1f3xzELkVtap.bo/qWRNWj05GtF/hq" ^
  WOMPI_PUBLIC_KEY="pub_test_wompi_dev" ^
  WOMPI_PRIVATE_KEY="prv_test_wompi_dev" ^
  WOMPI_INTEGRITY_SECRET="int_test_wompi_dev" ^
  WOMPI_WEBHOOK_SECRET="wh_test_wompi_dev" ^
  NOTIFICATION_API_KEY="notification_dev_key" ^
  DATACREDITO_CLIENT_SECRET="mock_datacredito_secret" ^
  GEMINI_API_KEY="dev_gemini_key" ^
  MAIL_PASSWORD="dev_mail_password"

if errorlevel 1 (
    echo.
    echo ERROR: no se pudieron insertar los secretos. Revisa que el contenedor met-vault este arriba.
    exit /b 1
)

echo.
echo Secretos insertados exitosamente en Vault.
echo Reinicia el backend para que los tome:  docker compose up -d backend
endlocal
