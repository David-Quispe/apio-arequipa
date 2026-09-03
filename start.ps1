# Levanta todo el stack de Proyecto APIO en local: PostgreSQL+PostGIS,
# GraphHopper y el backend. El frontend se deja aparte (npm run dev) porque
# normalmente se quiere ver su output en la propia terminal.

$ErrorActionPreference = "Stop"
$root = $PSScriptRoot

function Test-Puerto($port) {
    (Test-NetConnection -ComputerName localhost -Port $port -InformationLevel Quiet -WarningAction SilentlyContinue)
}

function Esperar-Puerto($port, $nombre, $segundos = 60) {
    for ($i = 0; $i -lt $segundos; $i++) {
        if (Test-Puerto $port) { return $true }
        Start-Sleep -Seconds 1
    }
    Write-Warning "$nombre no respondio en el puerto $port tras $segundos s"
    return $false
}

# --- 1. Docker Desktop + PostgreSQL/PostGIS ---
Write-Host "Verificando Docker..." -ForegroundColor Cyan
try {
    docker ps *> $null
} catch {
    Write-Host "Iniciando Docker Desktop (puede tardar un minuto)..." -ForegroundColor Yellow
    Start-Process "C:\Program Files\Docker\Docker\Docker Desktop.exe"
    do {
        Start-Sleep -Seconds 3
        docker ps *> $null
        $listo = $?
    } until ($listo)
}
docker compose -f "$root\infra\docker-compose.yml" up -d

# --- 2. GraphHopper ---
if (Test-Puerto 8989) {
    Write-Host "GraphHopper ya esta corriendo (puerto 8989)." -ForegroundColor Green
} else {
    Write-Host "Iniciando GraphHopper..." -ForegroundColor Cyan
    Start-Process -WindowStyle Hidden -WorkingDirectory "$root\routing" -FilePath "java" `
        -ArgumentList "-Xmx3g", "-jar", "graphhopper-web-11.0.jar", "server", "config.yml" `
        -RedirectStandardOutput "$root\routing\gh.log" -RedirectStandardError "$root\routing\gh-err.log"
    Esperar-Puerto 8989 "GraphHopper" | Out-Null
}

# --- 3. Backend (FastAPI) ---
if (Test-Puerto 8000) {
    Write-Host "El backend ya esta corriendo (puerto 8000)." -ForegroundColor Green
} else {
    if (-not (Test-Path "$root\backend\.env")) {
        Write-Warning "Falta backend\.env (copialo de .env.example y completa TOMTOM_API_KEY)"
    }
    Write-Host "Iniciando backend..." -ForegroundColor Cyan
    Start-Process -WindowStyle Hidden -WorkingDirectory "$root\backend" -FilePath "$root\backend\.venv\Scripts\python.exe" `
        -ArgumentList "-m", "uvicorn", "app.main:app", "--port", "8000"
    Esperar-Puerto 8000 "Backend" | Out-Null
}

Write-Host "`nListo:" -ForegroundColor Green
Write-Host "  PostgreSQL+PostGIS -> localhost:5432"
Write-Host "  GraphHopper        -> http://localhost:8989"
Write-Host "  Backend            -> http://localhost:8000/api/health"
Write-Host "`nFalta el frontend:" -ForegroundColor Yellow
Write-Host "  cd frontend; npm run dev"
