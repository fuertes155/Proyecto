@echo off
echo ===================================================
echo   Iniciando Proyecto Finanzas (MET Platform)
echo ===================================================

echo.
echo [1/3] Levantando Vault, base de datos y Redis...
docker compose up -d vault postgres redis

echo.
echo [2/3] Insertando secretos de desarrollo en Vault...
call setup_vault_dev.bat
if errorlevel 1 (
    echo ERROR: no se pudo configurar Vault. Abortando.
    pause
    exit /b 1
)

echo.
echo Levantando Backend y Notificaciones...
docker compose up -d --build

echo.
echo [3/3] Iniciando la aplicacion movil con Flutter...
cd mobile\met_app
echo Por favor, asegurate de tener un emulador Android/iOS abierto o un dispositivo conectado.
flutter run

echo.
echo Aplicacion iniciada correctamente.
pause
