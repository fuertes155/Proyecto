# ==========================================================================
#  Muestra la URL que hay que pegar en la app movil (pantalla "Servidor (dev)"
#  del login) para que el telefono llegue al backend.
#
#  Preferimos la IP de Tailscale: es fija por dispositivo y funciona desde
#  cualquier red (incluso datos moviles), sin tocar el firewall.
# ==========================================================================

$ErrorActionPreference = 'SilentlyContinue'
$ts = 'C:\Program Files\Tailscale\tailscale.exe'
$port = 8080

function Test-Backend($ip) {
    try {
        $r = Invoke-RestMethod -Uri "http://$ip`:$port/api/actuator/health" -TimeoutSec 4
        return "OK ($($r.status))"
    } catch {
        return "NO responde  ->  arranca el backend:  docker compose up -d backend"
    }
}

Write-Host ""
Write-Host "===================== URL DEL SERVIDOR PARA LA APP =====================" -ForegroundColor Cyan
Write-Host ""

# ---- Tailscale ----
$tsIp = $null
if (Test-Path $ts) {
    $status = & $ts status 2>&1 | Out-String
    if ($status -match 'Logged out') {
        Write-Host "[Tailscale] Sesion NO iniciada." -ForegroundColor Yellow
        Write-Host "  Ejecuta:  & '$ts' login" -ForegroundColor Yellow
        Write-Host "  (se abre el navegador; inicia sesion con Google/GitHub/Microsoft)"
    } else {
        $tsIp = (& $ts ip -4 2>&1 | Select-Object -First 1).Trim()
        if ($tsIp -match '^\d+\.\d+\.\d+\.\d+$') {
            Write-Host "[Tailscale]  IP de esta PC : $tsIp" -ForegroundColor Green
            Write-Host "             backend       : $(Test-Backend $tsIp)"
            Write-Host ""
            Write-Host "  >>> PEGA ESTO EN LA APP (login -> icono servidor):" -ForegroundColor Green
            Write-Host "      http://$tsIp`:$port/api" -ForegroundColor White -BackgroundColor DarkGreen
            Write-Host ""
            Write-Host "  El telefono tambien necesita la app Tailscale instalada y"
            Write-Host "  con la MISMA cuenta iniciada."
        }
    }
} else {
    Write-Host "[Tailscale] No instalado.  winget install Tailscale.Tailscale" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "----------------------------------------------------------------------"
Write-Host ""

# ---- LAN (alternativa: mismo Wi-Fi + firewall abierto en el 8080) ----
$lan = Get-NetIPAddress -AddressFamily IPv4 |
    Where-Object { $_.IPAddress -notmatch '^(127\.|169\.254\.|172\.(1[6-9]|2\d|3[01])\.)' -and $_.PrefixOrigin -ne 'WellKnown' } |
    Select-Object -ExpandProperty IPAddress -Unique
foreach ($ip in $lan) {
    Write-Host "[LAN] $ip  ->  http://$ip`:$port/api   backend: $(Test-Backend $ip)"
}
Write-Host ""
Write-Host "  (La opcion LAN solo sirve si el telefono esta en la misma Wi-Fi y"
Write-Host "   el puerto $port esta permitido en el Firewall de Windows.)"
Write-Host ""
