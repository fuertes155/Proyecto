@echo off
REM Publica el backend con Tailscale Funnel y recompila las 2 APKs.
REM Requisito previo (una vez):  "C:\Program Files\Tailscale\tailscale.exe" login
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0publicar-apks.ps1"
pause
