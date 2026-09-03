# Detiene GraphHopper y el backend (deja PostgreSQL/Docker corriendo, ya que
# no cuesta nada tenerlo prendido). Pasa -Todo para bajar tambien la base de datos.

param([switch]$Todo)

function Detener-Puerto($port, $nombre) {
    $conn = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    if ($conn) {
        Stop-Process -Id $conn.OwningProcess -Force
        Write-Host "$nombre detenido (puerto $port)." -ForegroundColor Green
    } else {
        Write-Host "$nombre no estaba corriendo." -ForegroundColor Yellow
    }
}

Detener-Puerto 8989 "GraphHopper"
Detener-Puerto 8000 "Backend"

if ($Todo) {
    docker compose -f "$PSScriptRoot\infra\docker-compose.yml" down
    Write-Host "PostgreSQL/PostGIS detenido." -ForegroundColor Green
}
