# Documentación de API - MET Platform

Esta carpeta contiene la documentación principal para consumir los servicios del backend (`met-platform`).

## 1. Postman Collection
Hemos exportado una colección de Postman pre-configurada que incluye las peticiones más comunes (Auth, Accounts, Transfers, Admin). 
Puedes importarla directamente desde Postman seleccionando el archivo:
`met-platform-postman-collection.json`

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
