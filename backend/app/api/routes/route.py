import httpx
from fastapi import APIRouter, HTTPException

from app.core.config import settings

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
    return {
        "distance_m": path["distance"],
        "time_s": path["time"] / 1000,
        "geometry": path["points"],
    }
