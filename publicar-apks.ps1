# ==========================================================================
#  Publica el backend con Tailscale Funnel (URL HTTPS fija) y recompila las
#  2 APKs apuntando a esa URL. Instalas los APK normal y funcionan desde
#  CUALQUIER red, sin configurar nada en el telefono.
#
#  Requisito: "tailscale login" ya hecho (ver CONEXION-MOVIL.md).
# ==========================================================================

$ErrorActionPreference = 'Stop'
$ts       = 'C:\Program Files\Tailscale\tailscale.exe'
$repo     = $PSScriptRoot
$appUser  = Join-Path $repo 'mobile\met_app'
$appAdmin = Join-Path $repo 'web_admin'
$outDir   = (Resolve-Path (Join-Path $repo '..\APKs_Listas')).Path
$port     = 8080

function Fail($m) { Write-Host ''; Write-Host "ERROR: $m" -ForegroundColor Red; exit 1 }

# ---- 0. Tailscale conectado -------------------------------------------
if (-not (Test-Path $ts)) { Fail 'Tailscale no instalado.  winget install Tailscale.Tailscale' }

$st = & $ts status --json 2>$null | ConvertFrom-Json
if ($st.BackendState -ne 'Running') {
    Write-Host ''
    Write-Host 'Tailscale no esta conectado. Ejecuta:' -ForegroundColor Yellow
    Write-Host "  & '$ts' login"
    Write-Host 'y abre el enlace que imprime para autorizar este equipo. Luego repite.'
    exit 1
}

$tsHost    = $st.Self.DNSName.TrimEnd('.')
$publicUrl = "https://$tsHost"
$apiBase   = "$publicUrl/api"
Write-Host ''
Write-Host "URL publica de este equipo:  $publicUrl" -ForegroundColor Cyan

# ---- 1. Backend local arriba -----------------------------------------
try {
    if ((Invoke-RestMethod "http://localhost:$port/api/actuator/health" -TimeoutSec 5).status -ne 'UP') { throw }
} catch { Fail "Backend no responde en localhost:$port.  docker compose up -d backend" }
Write-Host 'Backend local: OK' -ForegroundColor Green

# ---- 2. Encender Funnel ---------------------------------------------
Write-Host "Activando Tailscale Funnel (puerto $port) ..."
$f = (& $ts funnel --bg $port 2>&1 | Out-String)
Write-Host $f

Start-Sleep 3
$funnelUp = ((& $ts funnel status 2>&1 | Out-String) -match [regex]::Escape($tsHost))
if (-not $funnelUp) {
    Write-Host ''
    Write-Host 'Funnel no quedo activo. Falta permitirlo en tu tailnet (una sola vez):' -ForegroundColor Yellow
    Write-Host '  1. Abre  https://login.tailscale.com/admin/acls/file'
    Write-Host '  2. Agrega este bloque al JSON (a nivel raiz) y dale Save:'
    Write-Host ''
    Write-Host '        "nodeAttrs": [' -ForegroundColor Cyan
    Write-Host '          { "target": ["autogroup:member"], "attr": ["funnel"] }' -ForegroundColor Cyan
    Write-Host '        ],' -ForegroundColor Cyan
    Write-Host ''
    Write-Host '  3. Vuelve a correr este script.'
    exit 1
}

# ---- 3. Esperar a que la URL publica responda ----------------------
Write-Host "Esperando a que $publicUrl responda (hasta 3 min la primera vez por el certificado)..."
$ready = $false
foreach ($i in 1..18) {
    try {
        if ((Invoke-RestMethod "$apiBase/actuator/health" -TimeoutSec 10).status -eq 'UP') { $ready = $true; break }
    } catch { }
    Start-Sleep 10
    Write-Host ("  ...{0}s" -f ($i * 10))
}
if (-not $ready) { Fail "El backend no responde por $publicUrl. Prueba: curl $apiBase/actuator/health" }
Write-Host "Backend via $publicUrl : OK" -ForegroundColor Green

# ---- 4. Compilar las 2 APKs --------------------------------------
Write-Host ''
Write-Host '==> App Usuarios ...' -ForegroundColor Cyan
Push-Location $appUser
try {
    & flutter build apk --debug --dart-define=API_BASE_URL=$apiBase
    if ($LASTEXITCODE -ne 0) { Fail 'build met_app fallo' }
} finally { Pop-Location }
Copy-Item "$appUser\build\app\outputs\flutter-apk\app-debug.apk" (Join-Path $outDir 'App_Usuarios.apk') -Force
Write-Host 'OK -> App_Usuarios.apk' -ForegroundColor Green

Write-Host ''
Write-Host '==> App Administrador ...' -ForegroundColor Cyan
Push-Location $appAdmin
try {
    & flutter build apk --debug --dart-define=API_BASE_URL=$apiBase --dart-define=API_URL="$apiBase/v1/admin"
    if ($LASTEXITCODE -ne 0) { Fail 'build web_admin fallo' }
} finally { Pop-Location }
Copy-Item "$appAdmin\build\app\outputs\flutter-apk\app-debug.apk" (Join-Path $outDir 'App_Administrador.apk') -Force
Write-Host 'OK -> App_Administrador.apk' -ForegroundColor Green

# ---- 5. Fin -----------------------------------------------------
Write-Host ''
Write-Host '=======================================================================' -ForegroundColor Green
Write-Host " LISTO. Instala los APK de $outDir y funcionan en cualquier red."
Write-Host " Backend embebido en las apps:  $apiBase"
Write-Host ''
Write-Host " Apagar el acceso publico:   tailscale funnel reset"
Write-Host " Reactivarlo:                tailscale funnel --bg $port"
Write-Host '=======================================================================' -ForegroundColor Green
