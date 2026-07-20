# Met App

Aplicación móvil de la plataforma financiera Met.

## 🔒 Compilación de Producción (Seguridad y Ofuscación)

Para garantizar que la aplicación esté protegida contra ingeniería inversa y ataques Man-In-The-Middle, es **obligatorio** compilar la versión de producción (Release) utilizando ofuscación de código y proveyendo las variables de entorno necesarias (como el fingerprint del SSL).

### Android (APK / AppBundle)

```powershell
flutter build apk --release --obfuscate --split-debug-info=out/debug_info --dart-define=API_BASE_URL=https://api.tu-dominio.com --dart-define=SSL_FINGERPRINT=A1:B2:C3:D4:E5:F6:78:90:12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD:EF
```

Para mayor comodidad en Windows, puedes ejecutar el script de compilación seguro:
`.\build_prod_android.bat`

> **Nota:** Guarda siempre la carpeta `out/debug_info`. Si ocurre un crash en producción, necesitarás esos archivos para desofuscar el log de errores (stacktrace).
