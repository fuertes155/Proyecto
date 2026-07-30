# Documentación de API - MET Platform

Esta carpeta contiene la documentación principal para consumir los servicios del backend (`met-platform`).

## 1. Postman Collection
`met-platform-postman-collection.json` es una colección completa (99 requests, 13 carpetas) que cubre
**todos** los controladores REST del backend: Autenticación, Cuentas & Inicio, Transferencias & Depósitos,
Ahorro Programado, Solidaridad, Créditos Personales, Inversiones, Notificaciones, Legal, Soporte,
Webhooks & Pagos, Admin (11 subcarpetas) y Cumplimiento (reportes regulatorios).

Puntos clave de la colección:
- **Login automático**: al ejecutar `1. Autenticación > Login` (o `12.1 Autenticación > Login admin`),
  el `accessToken`/`refreshToken` se guardan solos en variables de colección — no hay que copiarlos a mano.
- **Firma HMAC automática**: todo POST/PUT/PATCH/DELETE (salvo auth, webhooks y admin/auth) se firma
  con HMAC-SHA256 vía un pre-request script a nivel de colección, replicando `HmacSignatureFilter.java`.
  Ajusta la variable `hmacSecret` si tu `HMAC_SECRET` local no es el valor de desarrollo por defecto.
- **PIN cifrado con RSA**: `register`, `login`, `pin-recovery/reset` (y probablemente `pin`) requieren el
  PIN cifrado con la llave pública de `GET /v1/auth/public-key` — cada request que lo necesita lo indica
  en su descripción; la colección no lo cifra por ti.
- **Variables de colección** para IDs de recursos (`groupId`, `savingsAccountId`, `portfolioId`, etc.):
  quedan vacías por defecto, complétalas con los IDs que te devuelvan las respuestas de creación.

Puedes importarla directamente desde Postman seleccionando el archivo anterior.

> ⚠️ **Nota de seguridad**: `HmacSignatureFilter.java` acepta el valor literal `X-Signature: test-skip-hmac`
> para saltarse la validación de firma, sin ninguna guarda de perfil/entorno en el filtro (solo se usa en
> tests). Tal como está, cualquier cliente podría usarlo para evadir la integridad HMAC en producción. Vale
> la pena revisar y restringirlo (p. ej. `@Profile("!prod")` o una bandera de configuración). Esta colección
> no depende de ese atajo.

## 2. Swagger / OpenAPI (Desarrollo)
En el entorno de producción, la documentación de Swagger está **deshabilitada** por razones de seguridad (`springdoc.api-docs.enabled=false`). 
Sin embargo, en el entorno local (dev), puedes acceder a la interfaz de Swagger y a la especificación OpenAPI de la siguiente forma:

1. Levanta el servidor backend localmente (asegúrate de que el perfil `dev` esté activo, lo cual es el valor por defecto si usas el `application-dev.yml`).
2. Visita en tu navegador:
   - **Swagger UI:** `http://localhost:8080/api/swagger-ui.html`
   - **OpenAPI JSON:** `http://localhost:8080/api/v3/api-docs`

Desde la vista de Swagger UI puedes interactuar con todos los endpoints disponibles.

## 3. Ejemplos de uso rápido (cURL)

### Autenticación (Login)
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "documentType": "CC",
    "documentNumber": "123456",
    "pin": "1234" # En el cliente real, esto va encriptado con RSA
  }'
```

### Consultar Perfil
```bash
curl -X GET http://localhost:8080/api/v1/profile/me \
  -H "Authorization: Bearer <TU_ACCESS_TOKEN>"
```

### Consultar Cuenta (Balance)
```bash
curl -X GET http://localhost:8080/api/v1/accounts/me \
  -H "Authorization: Bearer <TU_ACCESS_TOKEN>"
```

### Solicitar Transferencia (Ejemplo)
```bash
curl -X POST http://localhost:8080/api/v1/transfers/request \
  -H "Authorization: Bearer <TU_ACCESS_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "destinationAccountId": "00000000-0000-0000-0000-000000000000",
    "amount": 50000,
    "concept": "Pago de servicios"
  }'
```
*(Nota: Esto requiere aprobación por OTP para ejecutarse finalmente).*
