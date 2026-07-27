#!/bin/bash
# ==============================================================================
# Script para sembrar la base de datos local de desarrollo.
# Ejecuta el archivo seed_data.sql en el contenedor de PostgreSQL.
# ==============================================================================

# Se asume que el contenedor postgres local se llama "met-postgres" o está en docker-compose
# Este script usa variables de entorno por defecto, ajusta según necesidad.

DB_HOST=${DB_HOST:-localhost}
DB_PORT=${DB_PORT:-5432}
DB_NAME=${DB_NAME:-met}
DB_USER=${DB_USER:-met}

echo "Ejecutando seed de datos en $DB_HOST:$DB_PORT/$DB_NAME..."

# Usa docker-compose si se encuentra, o psql si está instalado localmente.
if command -v docker-compose &> /dev/null && docker-compose ps | grep -q postgres; then
    docker-compose exec -T postgres psql -U "$DB_USER" -d "$DB_NAME" < infrastructure/db/seed/seed_data.sql
elif command -v docker &> /dev/null && docker ps | grep -q postgres; then
    docker exec -i $(docker ps -qf "name=postgres") psql -U "$DB_USER" -d "$DB_NAME" < infrastructure/db/seed/seed_data.sql
else
    # Ejecución directa con psql (requiere PGPASSWORD seteado o .pgpass)
    psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f infrastructure/db/seed/seed_data.sql
fi

echo "¡Seed ejecutado con éxito!"
