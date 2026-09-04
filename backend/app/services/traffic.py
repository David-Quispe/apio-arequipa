"""
Fase 3 - Trafico en tiempo real (TomTom Traffic Flow API).

Consulta la congestion actual en un punto representativo de cada avenida del
corredor piloto (mismos puntos ya verificados visualmente en la Fase 1, ver
memoria del proyecto). Cachea el resultado un rato para no gastar la cuota
gratuita de TomTom (2500 consultas/dia) si el frontend hace polling seguido.
"""

import asyncio
import time

import httpx

from app.core.config import settings

TOMTOM_URL = "https://api.tomtom.com/traffic/services/4/flowSegmentData/absolute/10/json"
CACHE_TTL_S = 60

# Mismos puntos usados y verificados visualmente en la rubrica de privilegios
# (Fase 1) para cada avenida del corredor.
PUNTOS_AVENIDAS = {
    "Av. Ejercito": (-16.3895, -71.5478),
    "Puente Grau": (-16.3928, -71.5390),
    "Av. Bolognesi": (-16.3870, -71.5380),
    "Av. Dolores": (-16.41157, -71.52850),
    "Av. Alcides Carrion": (-16.41211, -71.53549),
    "Av. Goyeneche": (-16.39920, -71.52535),
    "Peral": (-16.39592, -71.53164),
    "El Filtro": (-16.39347, -71.53035),
}

_cache: dict = {"timestamp": 0, "data": None}


def nivel(ratio: float) -> str:
    if ratio >= 0.75:
        return "baja"
    if ratio >= 0.5:
        return "media"
    return "alta"


def factor_ajuste_eta(ratio: float) -> float:
    """Convierte un ratio de velocidad (actual/libre) en un multiplicador de
    tiempo de viaje. Acotado para que datos ruidosos no disparen el ETA."""
    if ratio <= 0:
        return 1.0
    return max(1.0, min(2.0, 1 / ratio))


async def _consultar_avenida(client: httpx.AsyncClient, nombre: str, punto: tuple[float, float]) -> dict:
    lat, lon = punto
    try:
        resp = await client.get(
            TOMTOM_URL,
            params={"point": f"{lat},{lon}", "key": settings.tomtom_api_key},
            timeout=10,
        )
        resp.raise_for_status()
        flow = resp.json()["flowSegmentData"]
        current = flow["currentSpeed"]
        libre = flow["freeFlowSpeed"]
        ratio = current / libre if libre else 1.0
        return {
            "avenida": nombre,
            "current_speed_kmh": current,
            "free_flow_speed_kmh": libre,
            "ratio": round(ratio, 2),
            "nivel": nivel(ratio),
        }
    except Exception as e:
        return {"avenida": nombre, "error": str(e), "ratio": 1.0, "nivel": "sin_datos"}


async def obtener_trafico_corredor() -> list[dict]:
    ahora = time.time()
    if _cache["data"] is not None and (ahora - _cache["timestamp"]) < CACHE_TTL_S:
        return _cache["data"]

    async with httpx.AsyncClient() as client:
        resultados = list(
            await asyncio.gather(
                *(_consultar_avenida(client, nombre, punto) for nombre, punto in PUNTOS_AVENIDAS.items())
            )
        )

    _cache["data"] = resultados
    _cache["timestamp"] = ahora
    return resultados
