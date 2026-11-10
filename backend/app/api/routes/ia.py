from fastapi import APIRouter, HTTPException

from app.core.config import settings
from app.services.ia import IaNoDisponible, RecomendacionRequest, generar_recomendacion

router = APIRouter()


@router.post("/ia/recomendacion")
async def post_recomendacion(datos: RecomendacionRequest):
    try:
        texto = await generar_recomendacion(datos)
    except IaNoDisponible as e:
        # 503: la app lo toma como "IA no disponible" y se queda con el
        # mensaje de su heuristica local, sin mostrar un error.
        raise HTTPException(status_code=503, detail=str(e)) from e
    return {"texto": texto, "modelo": settings.gemini_model}
