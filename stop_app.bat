@echo off
echo ===================================================
echo   Deteniendo Proyecto Finanzas (MET Platform)
echo ===================================================

echo.
echo Deteniendo servicios en Docker...
docker compose down

echo.
echo Servicios detenidos correctamente.
pause
