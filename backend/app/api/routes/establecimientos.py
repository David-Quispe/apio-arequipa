from fastapi import APIRouter, HTTPException

from app.services.establecimientos import listar_establecimientos

router = APIRouter()


@router.get("/establecimientos")
async def get_establecimientos(solo_emergencia: bool = False):
    establecimientos = listar_establecimientos(solo_emergencia=solo_emergencia)
    if not establecimientos and not solo_emergencia:
        # el JSON todavia no se genero: la app cae a su lista local
        raise HTTPException(
            status_code=503,
            detail="Faltan los datos: corre scripts/importar_establecimientos.py",
        )
    return {"total": len(establecimientos), "establecimientos": establecimientos}
