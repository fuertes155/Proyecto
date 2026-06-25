@echo off
echo ===================================================
echo   Iniciando Proyecto Finanzas (MET Platform)
echo ===================================================

echo.
echo [1/2] Levantando servicios en Docker (Base de datos, Backend, Redis, Notificaciones)...
docker-compose up -d --build

echo.
echo [2/2] Iniciando la aplicacion movil con Flutter...
cd mobile\met_app
echo Por favor, asegurate de tener un emulador Android/iOS abierto o un dispositivo conectado.
flutter run

echo.
echo Aplicacion iniciada correctamente.
pause
