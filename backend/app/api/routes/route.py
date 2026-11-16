import httpx
from fastapi import APIRouter, HTTPException

from app.core.config import settings
from app.services.corridor_overlap import avenidas_en_ruta, avenidas_trafico_en_ruta
from app.services.privilegios import detalle_privilegios_por_avenida
from app.services.traffic import factor_ajuste_eta, obtener_trafico_corredor

router = APIRouter()


@router.get("/route")
async def get_route(origin_lat: float, origin_lon: float, dest_lat: float, dest_lon: float):
    params = {
        "point": [f"{origin_lat},{origin_lon}", f"{dest_lat},{dest_lon}"],
        "profile": "car",
        "points_encoded": "false",
    }
    try:
        async with httpx.AsyncClient() as client:
            response = await client.get(f"{settings.graphhopper_url}/route", params=params, timeout=10)
    except httpx.RequestError:
        raise HTTPException(
            status_code=502,
            detail="No se pudo conectar con el motor de ruteo (GraphHopper). ¿Está corriendo?",
        )

    if response.status_code != 200:
        try:
            hints = response.json().get("hints", [])
        except Exception:
            hints = []
        fuera_de_cobertura = {
            "com.graphhopper.util.exceptions.PointNotFoundException",
            "com.graphhopper.util.exceptions.PointOutOfBoundsException",
        }
        if any(h.get("details") in fuera_de_cobertura for h in hints):
            raise HTTPException(
                status_code=400,
                detail="Ese punto está fuera del área piloto que cubre APIO (Arequipa "
                "Metropolitana). Probá con un punto más cerca de la ciudad.",
            )
        raise HTTPException(status_code=502, detail="GraphHopper no pudo calcular la ruta")

    path = response.json()["paths"][0]
    time_s = path["time"] / 1000

    time_s_con_trafico = time_s
    privilegios_cruzados: list[dict] = []
    avenidas_trafico_cruzadas: list[str] = []
    try:
        # Conjunto amplio (toda Arequipa, sin privilegios documentados) para
        # el ajuste de ETA y el panel de trafico -- antes usaba el mismo
        # conjunto angosto de 8 avenidas que los chips de privilegio, asi
        # que una ruta que no tocara esas 8 (la mayoria, fuera del Cono
        # Norte) nunca ajustaba su ETA por trafico real. Ver
        # corridor_overlap.CASE_AVENIDA_TRAFICO.
        metros_trafico = avenidas_trafico_en_ruta(path["points"]["coordinates"])
        if metros_trafico:
            trafico = {t["avenida"]: t["ratio"] for t in await obtener_trafico_corredor()}
            total_metros = sum(metros_trafico.values())
            ratio_ponderado = sum(
                metros * trafico.get(avenida, 1.0) for avenida, metros in metros_trafico.items()
            ) / total_metros
            time_s_con_trafico = time_s * factor_ajuste_eta(ratio_ponderado)
            avenidas_trafico_cruzadas = sorted(metros_trafico, key=lambda a: -metros_trafico[a])

        # Conjunto angosto (8 avenidas con rubrica de campo) solo para los
        # chips de privilegio -- semantica sin cambios.
        metros_privilegio = avenidas_en_ruta(path["points"]["coordinates"])
        if metros_privilegio:
            detalle = detalle_privilegios_por_avenida()
            privilegios_cruzados = sorted(
                (
                    {"avenida": avenida, **detalle[avenida]}
                    for avenida in metros_privilegio
                    if avenida in detalle
                ),
                key=lambda p: -p["general"],
            )
    except Exception:
        pass  # sin datos de trafico/privilegios, se muestra solo el ETA base

    return {
        "distance_m": path["distance"],
        "time_s": time_s,
        "time_s_con_trafico": round(time_s_con_trafico, 1),
        "privilegios_cruzados": privilegios_cruzados,
        "avenidas_trafico_cruzadas": avenidas_trafico_cruzadas,
        "geometry": path["points"],
    }
