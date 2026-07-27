# 🚀 Guía de Despliegue en VPS — MET Platform

Todo el código ya está listo en el repositorio. Cuando tengas el VPS, solo sigue estos pasos en orden.

---

## 1. Preparar el VPS

Recomendamos **Ubuntu 24.04 LTS**. Proveedores sugeridos:
- **DigitalOcean** (Droplet de $24/mes — 2 vCPU, 4GB RAM)
- **Contabo** (más económico, buena relación precio/rendimiento para Colombia)
- **AWS Lightsail** (si ya tienes cuenta AWS)

Una vez que tengas el VPS, conéctate por SSH e instala Docker:

```bash
# Instalar Docker y Docker Compose
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER
newgrp docker

# Verificar instalación
docker --version
docker compose version
```

---

## 2. Subir el código al servidor

```bash
# Opción A — Clonar desde GitHub (recomendado)
git clone https://github.com/TU_ORG/TU_REPO.git /opt/met-platform
cd /opt/met-platform

# Opción B — Copiar archivos desde tu máquina local (si aún no tienes Git)
# scp -r "d:\Proyecto Finanzas" usuario@IP_DEL_VPS:/opt/met-platform
```

---

## 3. Generar el hash BCrypt del Super Admin

> [!CAUTION]
> Este paso es **obligatorio**. Sin esto, el sistema no arranca correctamente porque la migración V21 necesita el hash.

En tu VPS o en tu máquina local (con Apache instalado):

```bash
# Opción A — Con htpasswd (apache2-utils)
sudo apt install apache2-utils -y
htpasswd -bnBC 12 "" TuContraseñaSegura | tr -d ':\n' | sed 's/$2y/$2a/'

# Opción B — Con Python (si no tienes htpasswd)
python3 -c "import bcrypt; print(bcrypt.hashpw(b'TuContraseñaSegura', bcrypt.gensalt(12)).decode())"
```

Copia el hash resultante — lo necesitarás en el paso siguiente.

---

## 4. Configurar las variables de entorno de producción

```bash
cd /opt/met-platform

# Crear el archivo de producción a partir del ejemplo
cp .env.production .env.prod.local

# Editar con los valores reales
nano .env.prod.local
```

**Variables que DEBES cambiar:**

| Variable | Descripción |
|---|---|
| `DB_PASSWORD` | Contraseña de PostgreSQL (usa una segura) |
| `REDIS_PASSWORD` | Contraseña de Redis |
| `JWT_SECRET` | Clave JWT (mínimo 64 caracteres aleatorios) |
| `AES_KEY` | Clave AES-256 (exactamente 32 caracteres) |
| `SUPERADMIN_PASSWORD_HASH` | El hash BCrypt generado en el paso anterior |
| `CORS_ORIGINS` | Tu dominio real: `https://tucooperativa.com` |
| `API_BASE_URL` | URL de tu API: `https://api.tucooperativa.com` |
| `WOMPI_*` | Claves reales de producción de Wompi |
| `MAIL_*` | Credenciales SMTP reales |

---

## 5. Configurar SSL (HTTPS)

**Opción A — Certbot gratuito (Let's Encrypt):** Recomendado
```bash
sudo apt install certbot -y
sudo certbot certonly --standalone -d tucooperativa.com -d api.tucooperativa.com

# Los certificados quedan en /etc/letsencrypt/live/tucooperativa.com/
cp /etc/letsencrypt/live/tucooperativa.com/fullchain.pem /opt/met-platform/infrastructure/nginx/certs/cert.pem
cp /etc/letsencrypt/live/tucooperativa.com/privkey.pem /opt/met-platform/infrastructure/nginx/certs/key.pem
```

**Opción B — Cloudflare:** Descarga el certificado desde el panel de Cloudflare y cópialo a la misma carpeta.

---

## 6. Levantar el sistema

```bash
cd /opt/met-platform

# Construir y arrancar todos los servicios en segundo plano
docker compose -f docker-compose.prod.yml --env-file .env.prod.local up -d --build

# Ver el estado de los contenedores
docker compose -f docker-compose.prod.yml ps

# Ver logs en tiempo real (útil en el primer arranque)
docker compose -f docker-compose.prod.yml logs -f backend
```

El primer arranque tarda unos **2-3 minutos** porque Maven compila el backend y Flutter compila web_admin.

---

## 7. Verificar que todo funciona

```bash
# Health check del backend
curl https://tucooperativa.com/api/actuator/health
# Respuesta esperada: {"status":"UP"}
```

Visita en el navegador:
- **Portal público:** `https://tucooperativa.com`
- **Panel Admin:** `https://tucooperativa.com/admin`

---

## 8. Renovación automática de SSL (Let's Encrypt)

```bash
# Cron para renovar el certificado cada lunes a las 3 AM
(crontab -l 2>/dev/null; echo "0 3 * * 1 certbot renew --quiet && cp /etc/letsencrypt/live/tucooperativa.com/fullchain.pem /opt/met-platform/infrastructure/nginx/certs/cert.pem && cp /etc/letsencrypt/live/tucooperativa.com/privkey.pem /opt/met-platform/infrastructure/nginx/certs/key.pem && docker compose -f /opt/met-platform/docker-compose.prod.yml restart nginx") | crontab -
```

---

## Firewall recomendado

```bash
sudo ufw allow 22    # SSH
sudo ufw allow 80    # HTTP
sudo ufw allow 443   # HTTPS
sudo ufw enable
```

| Puerto | Servicio | Acceso |
|---|---|---|
| `80` / `443` | Nginx | ✅ Público |
| `5432` | PostgreSQL | ❌ Solo interno |
| `6379` | Redis | ❌ Solo interno |
| `8080` | Backend Java | ❌ Solo interno |
| `8090` | Notification Service | ❌ Solo interno |
