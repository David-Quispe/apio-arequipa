import httpx
from fastapi import APIRouter, HTTPException

from app.core.config import settings
from app.services.corridor_overlap import avenidas_en_ruta
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
        raise HTTPException(status_code=502, detail="GraphHopper no pudo calcular la ruta")

    path = response.json()["paths"][0]
    time_s = path["time"] / 1000

    time_s_con_trafico = time_s
    avenidas_cruzadas: list[str] = []
    try:
        metros_por_avenida = avenidas_en_ruta(path["points"]["coordinates"])
        if metros_por_avenida:
            trafico = {t["avenida"]: t["ratio"] for t in await obtener_trafico_corredor()}
            total_metros = sum(metros_por_avenida.values())
            ratio_ponderado = sum(
                metros * trafico.get(avenida, 1.0) for avenida, metros in metros_por_avenida.items()
            ) / total_metros
            time_s_con_trafico = time_s * factor_ajuste_eta(ratio_ponderado)
            avenidas_cruzadas = list(metros_por_avenida.keys())
    except Exception:
        pass  # sin datos de trafico, se muestra solo el ETA base

    return {
        "distance_m": path["distance"],
        "time_s": time_s,
        "time_s_con_trafico": round(time_s_con_trafico, 1),
        "avenidas_cruzadas": avenidas_cruzadas,
        "geometry": path["points"],
    }
