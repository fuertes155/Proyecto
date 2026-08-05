# MET Platform

Plataforma financiera digital (cuentas, transferencias, ahorro programado, créditos personales,
inversiones, solidaridad y cumplimiento regulatorio).

Monorepo con backend en Java/Spring Boot, apps cliente en Flutter, sitio público en Next.js y un
microservicio de notificaciones en Go.

## Arquitectura

```
                    ┌────────────┐
                    │   Nginx    │  (proxy + TLS, solo en prod)
                    └─────┬──────┘
           ┌──────────────┼───────────────┬─────────────────┐
           ▼              ▼               ▼                 ▼
    ┌─────────────┐ ┌───────────┐  ┌─────────────┐  ┌──────────────┐
    │  Backend    │ │Notification│  │ web_admin   │  │ web_comercial│
    │ (Spring Boot│ │ (Go)       │  │ (Flutter Web│  │  (Next.js)   │
    │  hexagonal) │ │            │  │  panel admin│  │  sitio público│
    └──────┬──────┘ └───────────┘  └─────────────┘  └──────────────┘
           │
     ┌─────┴─────┬───────────┐
     ▼           ▼           ▼
 PostgreSQL   Redis        Vault
 (datos)     (cache/rate   (gestor de secretos)
              limiting)
```

La app móvil (`mobile/met_app`, Flutter) consume el mismo backend vía `http://<host>/api`.

## Estructura del repositorio

| Ruta | Descripción |
|---|---|
| [backend/met-platform](backend/met-platform) | API REST en Spring Boot 3 (Java 17), arquitectura hexagonal (`domain` / `application` / `infrastructure`) |
| [mobile/met_app](mobile/met_app) | App móvil de clientes (Flutter) |
| [web_admin](web_admin) | Panel administrativo web (Flutter Web) |
| [web_comercial](web_comercial) | Sitio público / comercial (Next.js 16 + React 19) |
| [services/notification-service](services/notification-service) | Microservicio de push notifications (Go) |
| [infrastructure](infrastructure) | Manifiestos Kubernetes, Nginx, monitoreo (Prometheus) |
| [docs](docs) | Documentación de API (Postman, Swagger) y guía de despliegue |
| [scripts](scripts) | Scripts de utilidad (seed de datos locales) |

Cada módulo de Flutter/Next.js tiene su propio `README.md` con detalles específicos.

### Backend: arquitectura hexagonal

`backend/met-platform/src/main/java/com/cooperativa/met/` está organizado por **capas** y dentro
de cada capa por **dominio de negocio** (`account`, `lending`, `savings`, `investment`,
`solidarity`, `compliance`, `identity`, `admin`, `bank`, `legal`, `notification`, `support`):

- `domain/` — modelo de negocio y puertos, sin dependencias de framework.
- `application/` — casos de uso, DTOs, mappers.
- `infrastructure/` — adaptadores (persistencia JPA, seguridad, web/controllers, cache, mensajería,
  scheduler, storage).

## Requisitos previos

- **Docker** y **Docker Compose** (v2)
- **JDK 17** (para correr el backend fuera de Docker) + Maven (o usa `mvnw` si existe en `backend/met-platform`)
- **Flutter** (SDK Dart `>=3.5.0`, canal `stable`) — para `mobile/met_app` y `web_admin`
- **Node.js 20** — para `web_comercial`
- **Go 1.23** — solo si vas a tocar `notification-service`

## Puesta en marcha local

### 1. Variables de entorno

```bash
cp .env.example .env
```

Los valores por defecto sirven para desarrollo. Las contraseñas/claves reales de dev se cargan en
Vault en el paso 3 (no van en `.env`).

### 2. Levantar infraestructura base

```bash
docker compose up -d vault postgres redis
```

### 3. Insertar secretos de desarrollo en Vault

Vault corre en modo `dev` (los secretos viven en memoria y se pierden al reiniciar el contenedor).

```bash
# Windows
setup_vault_dev.bat

# Linux/macOS
./setup_vault_dev.sh
```

> Si reinicias el contenedor `met-vault` (o Docker Desktop), vuelve a correr este script — si no,
> el backend falla al arrancar con `Invalid met.security.jwt.secret`.

### 4. Backend + servicio de notificaciones

```bash
docker compose up -d --build
```

Backend queda en `http://localhost:8080/api` (health check: `GET /api/actuator/health`).

Alternativa sin Docker (usa el `.env` de la raíz):
```bash
cd backend/met-platform
run_backend.cmd   # Windows; requiere JAVA_HOME configurado
```

### 5. Apps cliente

```bash
# App móvil (necesita un emulador/dispositivo activo)
cd mobile/met_app
flutter pub get
flutter run

# Panel admin (Flutter Web)
cd web_admin
flutter pub get
flutter run -d chrome

# Sitio comercial (Next.js)
cd web_comercial
npm install
npm run dev   # http://localhost:3000
```

### Todo en uno (Windows, dev)

`start_app.bat` encadena los pasos 2-4 y luego lanza `flutter run` para la app móvil.

### Sembrar datos de prueba

```bash
scripts/seed_local.sh
```

## Testing

| Módulo | Comando |
|---|---|
| Backend | `mvn -pl met-platform -am verify` (desde `backend/`) — compila, corre tests, JaCoCo, OWASP dependency-check y SpotBugs/FindSecBugs |
| Backend (solo tests) | `mvn -pl met-platform -am test` |
| Mobile | `flutter test` (desde `mobile/met_app/`) |
| Web admin | `flutter test` (desde `web_admin/`) |
| Web comercial | `npm run lint && npm run build` (desde `web_comercial/`) |
| Notification service | `go vet ./... && go test ./...` (desde `services/notification-service/`) |

El pipeline de CI (`.github/workflows/ci.yml`) corre estos mismos checks por módulo en cada push/PR
a `main`/`develop`, más un análisis de seguridad diario (`security-analysis.yml`).

## Documentación de API

- **Swagger UI** (solo perfil `dev`): `http://localhost:8080/api/swagger-ui.html`
- **Colección Postman**: [docs/api/met-platform-postman-collection.json](docs/api/met-platform-postman-collection.json)
  (login automático, firma HMAC automática por pre-request script). Ver [docs/api/README.md](docs/api/README.md).

Notas de seguridad de la API que afectan a cualquier cliente nuevo:
- Todo `POST`/`PUT`/`PATCH`/`DELETE` (salvo auth/webhooks/admin-auth) requiere firma **HMAC-SHA256** (`HmacSignatureFilter`).
- El PIN de usuario viaja cifrado con la llave pública RSA de `GET /v1/auth/public-key`.
- Autenticación por JWT con refresh token rotativo y revocación persistida.

## Despliegue

Ver [docs/DEPLOY.md](docs/DEPLOY.md) para la guía paso a paso de despliegue en VPS con
`docker-compose.prod.yml` (incluye backend, notification-service, web-admin, web-comercial y nginx
con TLS vía Let's Encrypt).

Para Kubernetes, los manifiestos están en [infrastructure/k8s](infrastructure/k8s).

## Integraciones externas

- **Wompi** — pasarela de pagos (checkout + payouts).
- **DataCrédito Experian** — consulta de central de riesgo. `CREDIT_BUREAU_PROVIDER=mock` en dev
  (el contrato con el proveedor real aún no está firmado; no asumas detalles del adaptador real
  sin confirmarlo).
- **Google Gemini** — funcionalidad asistida por IA.
- **Firebase Cloud Messaging** — push notifications (vía `notification-service`); en dev puede
  arrancar sin credenciales (`FCM_OPTIONAL=true`).
- **SMTP (Gmail)** — envío de correos; requiere una App Password de Google, no la contraseña normal.

## Observabilidad

- **Actuator + Prometheus**: métricas expuestas por el backend, alertas en [infrastructure/monitoring/prometheus-alerts.yml](infrastructure/monitoring/prometheus-alerts.yml).
- **Zipkin/Brave**: trazabilidad distribuida (configurable por `ZIPKIN_ENDPOINT`).
- **SonarCloud**: análisis estático configurado en [sonar-project.properties](sonar-project.properties).
