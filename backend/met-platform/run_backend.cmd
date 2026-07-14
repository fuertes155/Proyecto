@echo off
REM ==========================================
REM = WARNING: DEV ONLY. DO NOT USE IN PROD! =
REM ==========================================
setlocal DisableDelayedExpansion

REM Cargar variables de entorno desde el archivo .env
if exist ".env" (
    echo Cargando variables desde el archivo .env...
    for /f "usebackq tokens=1,* delims==" %%a in (".env") do (
        set "%%a=%%b"
    )
) else (
    echo Advertencia: No se encontro el archivo .env. Asegurate de que las variables requeridas esten configuradas.
)

REM Verificar que JAVA_HOME este configurado
if "%JAVA_HOME%"=="" (
    echo Error: La variable de entorno JAVA_HOME no esta configurada en el sistema.
    echo Por favor, configura JAVA_HOME para que apunte a tu instalacion del JDK.
    exit /b 1
)

REM Ejecutar Spring Boot (Usa mvnw si existe, o mvn del sistema en caso contrario)
if exist "mvnw.cmd" (
    echo Usando Maven Wrapper del proyecto...
    call mvnw.cmd spring-boot:run
) else (
    echo Usando Maven del sistema...
    call mvn spring-boot:run
)
