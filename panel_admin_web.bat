@echo off
REM ==========================================================================
REM  Compila el panel administrativo (web_admin) para navegador y lo publica en
REM  http://localhost:8081/admin/  (contenedor admin-web de docker-compose.yml).
REM
REM  Requiere el backend arriba (start_app.bat o: docker compose up -d backend).
REM  Vuelve a correrlo cada vez que cambie el codigo de web_admin.
REM ==========================================================================
setlocal

echo Compilando el panel administrativo para web...
pushd "%~dp0web_admin"
call flutter build web --release --base-href /admin/
if errorlevel 1 (
    popd
    echo ERROR: no se pudo compilar el panel administrativo.
    pause
    exit /b 1
)
popd

echo.
echo Iniciando el servidor web del panel...
pushd "%~dp0"
docker compose up -d admin-web
if errorlevel 1 (
    popd
    echo ERROR: no se pudo iniciar el contenedor admin-web. Revisa que Docker Desktop este abierto.
    pause
    exit /b 1
)
popd

echo.
echo Panel administrativo listo:  http://localhost:8081/admin/
start "" "http://localhost:8081/admin/"
endlocal
