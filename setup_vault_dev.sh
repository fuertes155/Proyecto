#!/bin/bash
# =========================================================================
# Script de Configuración Local de HashiCorp Vault para Met Platform
#
# IMPORTANTE: el contenedor met-vault corre en modo "dev" y guarda los
# secretos EN MEMORIA. Cada vez que se reinicia (docker compose down/up,
# reinicio de Docker Desktop, reinicio del PC) los secretos se pierden y el
# backend falla al arrancar con:
#   "Invalid met.security.jwt.secret: must be at least 256-bit / 32 bytes"
# Vuelve a ejecutar este script después de cada reinicio de Vault.
# =========================================================================
set -e

# El token debe coincidir con VAULT_TOKEN del .env de la raíz.
: "${VAULT_TOKEN:=root-local-2026}"
# Sin VAULT_ADDR el CLI asume https:// y falla contra el servidor dev (HTTP).
VAULT_ENV=(-e VAULT_ADDR=http://127.0.0.1:8200 -e "VAULT_TOKEN=${VAULT_TOKEN}")

echo "Esperando a que Vault inicie..."
sleep 5

echo "Habilitando el motor de secretos KV v2..."
docker exec "${VAULT_ENV[@]}" met-vault vault secrets enable -path=secret kv-v2 || echo "El motor ya está habilitado o hubo un error"

echo "Insertando secretos de desarrollo para met-platform..."

# Nota: se usan comillas simples para que bash no interprete los '$' del hash BCrypt.
docker exec "${VAULT_ENV[@]}" met-vault vault kv put secret/met-platform \
  DB_PASSWORD='MetDev2026Secure' \
  REDIS_PASSWORD='redis-dev-pass' \
  JWT_SECRET='SUPER_SECRET_JWT_KEY_FOR_DEV_MIN_256_BITS_!@#$' \
  AES_KEY='AES_32_BYTES_KEY_FOR_DEV_LOCAL!!' \
  HMAC_SECRET='D3vHmacS3cr3tKey123!@#' \
  SUPERADMIN_PASSWORD_HASH='$2a$12$EOKeWz34L2tNqGOimqGBjeo1f3xzELkVtap.bo/qWRNWj05GtF/hq' \
  WOMPI_PUBLIC_KEY='pub_test_wompi_dev' \
  WOMPI_PRIVATE_KEY='prv_test_wompi_dev' \
  WOMPI_INTEGRITY_SECRET='int_test_wompi_dev' \
  WOMPI_WEBHOOK_SECRET='wh_test_wompi_dev' \
  NOTIFICATION_API_KEY='notification_dev_key' \
  DATACREDITO_CLIENT_SECRET='mock_datacredito_secret' \
  GEMINI_API_KEY='dev_gemini_key' \
  MAIL_PASSWORD='dev_mail_password'

echo "¡Secretos insertados exitosamente en Vault!"
echo "Reinicia el backend para que los tome:  docker compose up -d backend"
