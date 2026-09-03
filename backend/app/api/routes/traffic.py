from fastapi import APIRouter

from app.services.traffic import obtener_trafico_corredor

router = APIRouter()


@router.get("/traffic")
async def get_traffic():
    return {"avenidas": await obtener_trafico_corredor()}
