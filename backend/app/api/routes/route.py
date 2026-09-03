import httpx
from fastapi import APIRouter, HTTPException

from app.core.config import settings
from app.services.corridor_overlap import avenidas_en_ruta
from app.services.privilegios import scores_por_avenida
from app.services.traffic import factor_ajuste_eta, obtener_trafico_corredor

router = APIRouter()


@router.get("/route")
async def get_route(origin_lat: float, origin_lon: float, dest_lat: float, dest_lon: float):
    params = {
        "point": [f"{origin_lat},{origin_lon}", f"{dest_lat},{dest_lon}"],
        "profile": "car",
        "points_encoded": "false",
    }
    async with httpx.AsyncClient() as client:
        response = await client.get(f"{settings.graphhopper_url}/route", params=params)

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
                detail="Ese punto está fuera del área piloto que cubre APIO (Cono Norte - cluster "
                "hospitalario del Cercado). Probá con un punto más cerca del corredor.",
            )
        raise HTTPException(status_code=502, detail="GraphHopper no pudo calcular la ruta")

    path = response.json()["paths"][0]
    time_s = path["time"] / 1000

    time_s_con_trafico = time_s
    privilegios_cruzados: list[dict] = []
    try:
        metros_por_avenida = avenidas_en_ruta(path["points"]["coordinates"])
        if metros_por_avenida:
            trafico = {t["avenida"]: t["ratio"] for t in await obtener_trafico_corredor()}
            total_metros = sum(metros_por_avenida.values())
            ratio_ponderado = sum(
                metros * trafico.get(avenida, 1.0) for avenida, metros in metros_por_avenida.items()
            ) / total_metros
            time_s_con_trafico = time_s * factor_ajuste_eta(ratio_ponderado)

            scores = scores_por_avenida()
            privilegios_cruzados = sorted(
                (
                    {"avenida": avenida, "score": scores[avenida]}
                    for avenida in metros_por_avenida
                    if avenida in scores
                ),
                key=lambda p: -p["score"],
            )
    except Exception:
        pass  # sin datos de trafico/privilegios, se muestra solo el ETA base

    return {
        "distance_m": path["distance"],
        "time_s": time_s,
        "time_s_con_trafico": round(time_s_con_trafico, 1),
        "privilegios_cruzados": privilegios_cruzados,
        "geometry": path["points"],
    }
