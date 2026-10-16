# Probar pagos con el sandbox de Wompi

El sandbox de Wompi procesa pagos de prueba con la API real, sin mover dinero.
El backend lo usa con el perfil de Spring **`sandbox`**, que se suma al perfil base:

| Dónde | `SPRING_PROFILES_ACTIVE` |
|---|---|
| Local (Docker) | `dev,sandbox` |
| Render | `prod,sandbox` |

Con `sandbox`, los depósitos (checkout y PSE nativo) van contra `https://sandbox.wompi.co/v1`.
Los retiros a banco externo siguen simulados: "Pagos a Terceros" es otro producto de Wompi
y todavía no tiene credenciales.

## 1. Llaves de sandbox

En [dashboard.wompi.co](https://dashboard.wompi.co), en modo **pruebas**, sección *Desarrolladores*:

| Variable | Dónde está | Empieza por |
|---|---|---|
| `WOMPI_PUBLIC_KEY` | Llaves del API → llave pública | `pub_test_` |
| `WOMPI_PRIVATE_KEY` | Llaves del API → llave privada | `prv_test_` |
| `WOMPI_INTEGRITY_SECRET` | Secretos para integración técnica → **Integridad** | `test_integrity_` |
| `WOMPI_WEBHOOK_SECRET` | Secretos para integración técnica → **Eventos** | `test_events_` |

`WOMPI_WEBHOOK_SECRET` es el secreto de **eventos**. Con el de integridad en su lugar, todos los webhooks se rechazan.

## 2. Una URL pública para el backend

Wompi necesita una URL pública por dos razones: enviar los eventos (webhooks) y devolver al usuario cuando termina de pagar.

**Opción A: local con un túnel**

```powershell
winget install --id Cloudflare.cloudflared
cloudflared tunnel --url http://localhost:8080
```

El comando imprime una URL `https://<algo>.trycloudflare.com`. Esa URL cambia cada vez que se reinicia el túnel; cuando cambie, actualiza los pasos 3 y 4.

**Opción B: Render.** La URL es `https://met-backend-y04c.onrender.com`.

## 3. Configurar el backend

**Local:** en el `.env` de la raíz del repo:

```env
SPRING_PROFILES_ACTIVE=dev,sandbox
WOMPI_PUBLIC_KEY=pub_test_...
WOMPI_PRIVATE_KEY=prv_test_...
WOMPI_INTEGRITY_SECRET=test_integrity_...
WOMPI_WEBHOOK_SECRET=test_events_...
WOMPI_REDIRECT_URL=https://<tu-url-publica>/api/v1/payments/wompi/return
```

Después, reconstruye y reinicia el backend (tarda unos 8 minutos):

```powershell
docker compose up -d --build backend
```

Las variables del `.env` tienen prioridad sobre los valores de ejemplo que `setup_vault_dev.bat` guarda en Vault.

**Render:** en el dashboard del servicio `met-backend`:

1. Cambia `SPRING_PROFILES_ACTIVE` a `prod,sandbox`.
2. Pon las 4 llaves. En `render.yaml` están como `sync: false`, así que el blueprint no las sobrescribe.

`WOMPI_REDIRECT_URL` ya viene configurada en el blueprint.

## 4. Registrar la URL de eventos en Wompi

En el dashboard de Wompi, en modo pruebas, ve a *Desarrolladores → URL de eventos* y registra:

```
https://<tu-url-publica>/api/v1/webhooks/payment
```

## 5. Comprobar el arranque

En los logs del backend (`docker compose logs -f backend`) deben aparecer estas dos líneas:

```
Wompi configurado contra https://sandbox.wompi.co/v1 (SANDBOX).
Catálogo PSE de Wompi sincronizado al arrancar: 2 bancos.
```

Si en su lugar aparece `Revisa la configuración de Wompi (SANDBOX): ...`, el mensaje dice qué llave falta, cuál tiene un valor de ejemplo o cuál es de otro ambiente. Los valores de las llaves nunca se escriben en el log.

## 6. Datos de prueba

| Medio | Aprobada | Rechazada |
|---|---|---|
| Tarjeta | `4242 4242 4242 4242` | `4111 1111 1111 1111` |
| Nequi | `3991111111` | `3992222222` |
| PSE (checkout) | banco "que aprueba" | banco "que rechaza" |
| PSE nativo (en la app) | **Banco que aprueba** (código `1`) | **Banco que rechaza** (código `2`) |

Para tarjetas, cualquier fecha futura y cualquier CVC sirven.

En el selector de PSE nativo de la app también aparecen los bancos reales (Bancolombia, Davivienda, etc.). En sandbox, Wompi los rechaza: elige siempre uno de los dos bancos de prueba.

## 7. Flujo esperado

1. En la app: **Depositar**, eliges el monto y se abre el checkout de Wompi en el navegador.
2. Pagas con un dato de prueba. Wompi te lleva a la página "Estamos confirmando tu pago".
3. Vuelves a la app y tocas **Ya pagué**. La pantalla de espera consulta el saldo.
4. Wompi envía `transaction.updated` a `/api/v1/webhooks/payment`. El backend verifica el checksum y acredita el depósito, y la pantalla de espera muestra el éxito.

La redirección de Wompi nunca acredita saldo. Solo lo hace el webhook firmado.

## Problemas frecuentes

| Síntoma en los logs | Causa |
|---|---|
| `Wompi checksum mismatch ... environment=test` | `WOMPI_WEBHOOK_SECRET` no es el secreto de eventos de sandbox. |
| `Wompi respondió 422 ... "signature"` | `WOMPI_INTEGRITY_SECRET` no corresponde a la llave pública. |
| `Wompi respondió 401` | Llaves de producción contra la URL de sandbox, o al revés. |
| No llega ningún evento | La URL de eventos no está registrada en Wompi, el túnel cambió de URL o Render estaba dormido. Wompi reintenta a los 30 min, 3 h y 24 h. |
| `El banco está tardando en responder` | Wompi no entregó la URL del banco a tiempo (PSE nativo). Reintenta. |

## Pendiente antes de producción

- **PSE nativo:** Wompi exige que el usuario acepte explícitamente dos documentos antes de pagar: la política de privacidad y la autorización de datos personales. El checkout hosteado de Wompi ya los muestra; la pantalla de PSE nativo de la app todavía no. Los enlaces a los PDF vienen en `GET /merchants/info`.
- **Retiros (Pagos a Terceros):** requieren activar el producto con Wompi y verificar `WompiPayoutAdapter` contra su sandbox.
