# Que las APKs funcionen en cualquier red

El problema: el APK trae la direccion del backend "quemada". Si es una IP de
tu Wi-Fi (`192.168.x.x`), cambia cada vez que cambias de red y la app deja de
conectar.

La solucion: darle al backend una **URL publica HTTPS fija** con **Tailscale
Funnel**, y compilar las APKs contra esa URL. Asi instalas las APKs de forma
normal y funcionan desde casa, la oficina o datos moviles, sin tocar nada en
el telefono.

> ngrok no sirve aqui: Windows tiene Smart App Control y bloquea su binario.
> Tailscale si esta permitido y ya quedo instalado en este PC.

---

## Pasos (una sola vez)

### 1. Iniciar sesion en Tailscale (en este PC)

```powershell
& "C:\Program Files\Tailscale\tailscale.exe" login
```

Se abre el navegador -> inicia sesion con Google / GitHub / Microsoft.

### 2. Habilitar Funnel en tu cuenta Tailscale (consola web, gratis)

- Abre <https://login.tailscale.com/admin/dns> -> activa **MagicDNS** y
  **HTTPS Certificates**.
- Abre <https://login.tailscale.com/admin/acls> y asegura que este equipo
  puede usar **Funnel** (Tailscale muestra el enlace exacto la primera vez
  que corras el script del paso 3 si falta algo).

### 3. Publicar y compilar (un comando)

```
Proyecto\publicar-apks.bat
```

Esto:
- enciende Tailscale Funnel para el puerto 8080 (queda activo de forma
  permanente, sobrevive reinicios),
- recompila **App_Usuarios.apk** y **App_Administrador.apk** apuntando a la
  URL publica `https://<equipo>.<tu-tailnet>.ts.net/api`,
- las deja en `C:\App\APKs_Listas\`.

### 4. Instalar

Copia los dos APK al telefono e instalalos normal. Listo: funcionan en
cualquier red.

---

## Uso diario

- **No hay que hacer nada.** La URL de Funnel no cambia aunque reinicies el PC
  o cambies de Wi-Fi.
- Credenciales de prueba: usuario `123456` / PIN `1234`; admin `admin` / `admin123`.

## Si apagas el PC

Las apps **dejan de funcionar** mientras el PC esté apagado — el backend corre en
el PC. Cuando lo enciendes otra vez, todo vuelve solo (contenedores con
`restart: unless-stopped`, Tailscale y Funnel arrancan con Windows), **siempre que
Docker Desktop esté puesto para arrancar con el sistema**:

> Docker Desktop → Settings → General → marcar **"Start Docker Desktop when you sign in"**

Tras encender el PC, dale ~1-2 min y las apps ya conectan. Si algo no levanta:
`docker compose up -d` en `C:\App\Proyecto`.

## Seguridad

Mientras Funnel este activo, el backend es accesible por internet (la URL no
esta indexada, y los endpoints siguen protegidos por login + firma HMAC +
rate-limiting). Es un backend de desarrollo con datos de prueba. Para apagar
el acceso publico cuando no estes probando:

```powershell
& "C:\Program Files\Tailscale\tailscale.exe" funnel reset
```

Y para volver a encenderlo: `& "C:\Program Files\Tailscale\tailscale.exe" funnel --bg 8080`

---

## Alternativas

- **Solo misma Wi-Fi, sin exponer a internet:** usa `tailscale serve` en vez
  de `funnel` (hay que instalar Tailscale tambien en el telefono, misma
  cuenta). O pega la IP LAN a mano en la app (login -> icono de servidor,
  solo en builds debug) y abre el puerto 8080 en el Firewall de Windows.
- **Sin depender de tu PC encendido:** desplegar el backend en un hosting
  (Render / Railway / Fly.io / VPS) y compilar con
  `--dart-define=API_BASE_URL=https://tu-backend/api`.

## Cambiar la URL sin recompilar (solo App Usuarios, builds debug)

En la pantalla de login hay un icono de servidor arriba a la derecha (solo
visible en debug): permite escribir otra URL de backend y la guarda en el
dispositivo. Util para pruebas rapidas.
