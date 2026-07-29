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
  RSA_PRIVATE_KEY="MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDOmaNevSuljuYbEIbQoqli8HyHBB9td2xijIJDeuDCCR5kvVQn7QyAnKu5fJ95mZvaHVA1TFbOyQ12yidf/GaSjv9RRAqeplCDEiRcSaWD+GNMjDRgQ1EEWetFHafszVU2+37XjtP1NJhIPXFuIIx1EKV4q94yM9RhNY4KtqyVc1hadWkbVP4Kav5kekSzB40efONepZSNarEUaK5KA22BNR9AzGeVNKh/+eas2YMGzxdXQrgDKCfEOrH1tDBm8xwj6FLnPKUgViNqAE7I1lXZoaiJWjs7FKxPQaPZ8Vfc7cTcF+BzJNLUDWv8lVhoGcnoQgLQ9H36PxImHv1BZBw9AgMBAAECggEABC9GCROM8jdqpmyT09Dno2uBlOBMnOKseavdzV7+IAHKnCRss2VwjtVy46qWbwJvnScMZvf3bikKzuJgrufQNWRpptNnqIz+ESfNBy9Ksme5PBA+8ZA2KVUt0OJ2HRAd4MAAcrdKcx0h8PH6/aMOBwpTnCQ41AyZXv4/Ci0uKQ6IDii4vzNt16uNbxnifIwnfOGkSrv2js0LeirPWNRjnmfMgsK4/xLbXaVbNqKLb3LsNB+DQq3k7AP1O1RrMnCfnAt2IaP20Z6RLiTYg+T6lFHh3oud1Fs463tvmJ20Msvkp0gJRr0RW8dKoLQukM96JNcS9CmEAiuPofg5IgKogQKBgQDvSNO0xN0z0lEGMXnaS2CFMdmq/PrIVRPo9vMiHePNAEPHNkpW2+e3gIwVwMRWiX0IFCUfnXo+JwCezy4RWoeDHaLlPEk9R42fm3J2EsVw2Ff0ZcxIrBreGNPylP7ePwEArVlReih/ZZTI6qxR8pTiCuo1CEdx7Xv2Pu1RQ6cjoQKBgQDdCE+GXkbRkjyvJ156b3UN2mCcL04buHKglRm3GQoMWPEf1a+akQBb2QpwWRcyJdNLYVdFDFZ0yOuUTkEmcm4+yn6xXEqtzcJW4qCIffq7e/stMFFMXk2JHxxLPaAOVnHltIPH+Ln3V6HZsNXcCHxB82+g7rSvdLTgQJ6MjdwzHQKBgQCwMk5B9Bx2Rk4HY3M4vPxr19NSR0pbLdqlVwKdgqj1kVZXYLDI2ND9nJiKhBGPL1p0EnFroEgdcBbS2fgwKxtR/wauCgBcMU4l0w8rxpLAj23ktvkAIkr+dAXNwM0T1gzk9MCqcGtVs/UlJTiSdzAOar8fIeOKDreTEa8qgciXQQKBgDrI+Q2NiVw3fng/CjuZ5RHYIJWpZyasOaBHx10RBcEe7pI+7MM5CVVkNgiHSUoHEVkc/G7axyusPCtnXKBzEqsUg/l7yEjNToB5KaTjjInMgZMVJKog8pIjegzwyN7HUud6yQmoNx13aw5Qn7AzeYi4y9mLYva/HVP/G6vaFtRpAoGBAMBEgaZC8QIOREaT+AjaIHIMmYTONJCdAxmaUxZCwfE0vz4K92yIk4mD2d9pMyKXWbgQJ2ikVRM2xqx0h+NO2uZeqf4s1sgukZ+pb2ohU+kAem49q7LpY6y9ms6H5Bz4+ZL2czDPKF0wMtpP2NIZUwgDoV6rP2w8vXvOUDzGAOJM" ^
  RSA_PUBLIC_KEY="MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAzpmjXr0rpY7mGxCG0KKpYvB8hwQfbXdsYoyCQ3rgwgkeZL1UJ+0MgJyruXyfeZmb2h1QNUxWzskNdsonX/xmko7/UUQKnqZQgxIkXEmlg/hjTIw0YENRBFnrRR2n7M1VNvt+147T9TSYSD1xbiCMdRCleKveMjPUYTWOCraslXNYWnVpG1T+Cmr+ZHpEsweNHnzjXqWUjWqxFGiuSgNtgTUfQMxnlTSof/nmrNmDBs8XV0K4AygnxDqx9bQwZvMcI+hS5zylIFYjagBOyNZV2aGoiVo7OxSsT0Gj2fFX3O3E3BfgcyTS1A1r/JVYaBnJ6EIC0PR9+j8SJh79QWQcPQIDAQAB" ^
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
