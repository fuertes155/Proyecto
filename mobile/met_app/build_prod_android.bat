@echo off
echo ========================================================
echo Compilando Met App para Android (Release Seguro)
echo ========================================================
echo.

:: Asegurarse de que el directorio de debug info exista
if not exist "out\debug_info" mkdir "out\debug_info"

:: TODO: Cambiar estas variables antes de pasar a produccion real
set API_URL=https://api.tu-dominio.com
set SSL_FINGERPRINT=A1:B2:C3:D4:E5:F6:78:90:12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD:EF
set HMAC_SECRET=ProdHmacSecretSuperSecure!

echo Aplicando Ofuscacion de Codigo...
echo.

call flutter build apk --release ^
  --obfuscate ^
  --split-debug-info=out/debug_info ^
  --dart-define=API_BASE_URL=%API_URL% ^
  --dart-define=SSL_FINGERPRINT=%SSL_FINGERPRINT% ^
  --dart-define=HMAC_SECRET=%HMAC_SECRET%

echo.
echo ========================================================
echo Terminado. El APK esta en build\app\outputs\flutter-apk\app-release.apk
echo Guarda los archivos en out\debug_info para desofuscar futuros errores de produccion.
echo ========================================================
pause
