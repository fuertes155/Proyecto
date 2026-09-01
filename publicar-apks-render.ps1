# ==========================================================================
#  Recompila las 2 APKs apuntando al backend en Render (nube).
#  A diferencia de publicar-apks.ps1 (Tailscale Funnel + PC encendida),
#  estas APKs funcionan desde cualquier red SIN tu PC.
#
#  Requiere: SDK de Android configurado. Usa C:\fl (junction al SDK de
#  Flutter) para esquivar el bug de objective_c con el espacio en la ruta
#  del usuario. Si no existe: New-Item -ItemType Junction -Path C:\fl -Target "$env:USERPROFILE\dev\flutter"
# ==========================================================================

$ErrorActionPreference = 'Stop'
$repo     = $PSScriptRoot
$appUser  = Join-Path $repo 'mobile\met_app'
$appAdmin = Join-Path $repo 'web_admin'
$outDir   = (Resolve-Path (Join-Path $repo '..\APKs_Listas')).Path
$flutter  = 'C:\fl\bin\flutter.bat'

$apiBase = 'https://met-backend-y04c.onrender.com/api'
$hmac    = '4a9bb7f1809e49109bfa94629c030cce20cdc0f8bddd3737508cdcbb9ec2a244'

if (-not (Test-Path $flutter)) { Write-Host "ERROR: no existe $flutter" -ForegroundColor Red; exit 1 }

Write-Host ''
Write-Host "Backend embebido:  $apiBase" -ForegroundColor Cyan

# ---- Verificar que el backend responde (y despertarlo si estaba dormido) ----
Write-Host 'Verificando backend (puede tardar ~4 min si estaba dormido)...'
$ok = $false
foreach ($i in 1..30) {
    try {
        if ((Invoke-RestMethod "$apiBase/actuator/health" -TimeoutSec 20).status -eq 'UP') { $ok = $true; break }
    } catch { }
    Start-Sleep 10
}
if (-not $ok) { Write-Host "ERROR: el backend no responde en $apiBase/actuator/health" -ForegroundColor Red; exit 1 }
Write-Host 'Backend: OK' -ForegroundColor Green

# ---- App Usuarios (met_app) ----
Write-Host ''
Write-Host '==> App Usuarios ...' -ForegroundColor Cyan
Push-Location $appUser
try {
    & $flutter build apk --debug `
        --dart-define=API_BASE_URL=$apiBase `
        --dart-define=HMAC_SECRET=$hmac
    if ($LASTEXITCODE -ne 0) { throw 'build met_app fallo' }
} finally { Pop-Location }
Copy-Item "$appUser\build\app\outputs\flutter-apk\app-debug.apk" (Join-Path $outDir 'App_Usuarios.apk') -Force
Write-Host 'OK -> App_Usuarios.apk' -ForegroundColor Green

# ---- App Administrador (web_admin como APK) ----
Write-Host ''
Write-Host '==> App Administrador ...' -ForegroundColor Cyan
Push-Location $appAdmin
try {
    & $flutter build apk --debug `
        --dart-define=API_BASE_URL=$apiBase `
        --dart-define=API_URL="$apiBase/v1/admin" `
        --dart-define=HMAC_SECRET=$hmac
    if ($LASTEXITCODE -ne 0) { throw 'build web_admin fallo' }
} finally { Pop-Location }
Copy-Item "$appAdmin\build\app\outputs\flutter-apk\app-debug.apk" (Join-Path $outDir 'App_Administrador.apk') -Force
Write-Host 'OK -> App_Administrador.apk' -ForegroundColor Green

Write-Host ''
Write-Host '=======================================================================' -ForegroundColor Green
Write-Host " LISTO. Instala los APK de $outDir"
Write-Host " Funcionan desde cualquier red, sin tu PC."
Write-Host " Backend: $apiBase"
Write-Host '=======================================================================' -ForegroundColor Green
