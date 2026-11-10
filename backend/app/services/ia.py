"""Recomendacion en lenguaje natural para quien conduce la ambulancia,
generada por un LLM (Google Gemini, free tier) a partir de los datos reales
que ya calcula APIO: ETA con trafico de TomTom, avenidas congestionadas que
cruza la ruta, privilegios de transito y el nivel de riesgo de la
heuristica on-device de la app (ia/EstimadorRiesgo.kt).

La API key vive solo aca (backend/.env, GEMINI_API_KEY) y nunca dentro del
APK. Si no hay key o Gemini falla, se lanza IaNoDisponible y la app sigue
mostrando el mensaje de su heuristica local, asi que la IA externa nunca
puede tirar abajo la pantalla.
"""

import time

import httpx
from pydantic import BaseModel, Field

from app.core.config import settings

GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/{modelo}:generateContent"

# La app vuelve a evaluar la ruta cada 25 s; para no gastar la cuota diaria
# del free tier, una misma situacion (mismo destino, mismo nivel de riesgo,
# mismo ETA redondeado) reusa la respuesta durante este tiempo.
CACHE_TTL_S = 180
_cache: dict[tuple, tuple[float, str]] = {}


class IaNoDisponible(Exception):
    pass


class AvenidaNivel(BaseModel):
    avenida: str
    nivel: str


class PrivilegioRuta(BaseModel):
    avenida: str
    tipo: str | None = None
    score: float


class RecomendacionRequest(BaseModel):
    destino: str
    distancia_m: float
    tiempo_s: float
    tiempo_s_con_trafico: float
    avenidas_trafico: list[AvenidaNivel] = Field(default_factory=list)
    privilegios: list[PrivilegioRuta] = Field(default_factory=list)
    riesgo_nivel: str
    riesgo_puntaje: float


def _clave_cache(datos: RecomendacionRequest) -> tuple:
    return (
        datos.destino,
        datos.riesgo_nivel,
        round(datos.tiempo_s_con_trafico / 60),
        tuple(sorted(f"{a.avenida}:{a.nivel}" for a in datos.avenidas_trafico)),
    )


def construir_prompt(datos: RecomendacionRequest) -> str:
    minutos_libre = datos.tiempo_s / 60
    minutos_trafico = datos.tiempo_s_con_trafico / 60
    km = datos.distancia_m / 1000

    if datos.avenidas_trafico:
        avenidas = ", ".join(f"{a.avenida} (trafico {a.nivel})" for a in datos.avenidas_trafico)
    else:
        avenidas = "ninguna avenida monitoreada con congestion"

    if datos.privilegios:
        privilegios = ", ".join(
            f"{p.avenida}: {p.tipo or 'privilegio general'} ({p.score:.1f}/10)" for p in datos.privilegios
        )
    else:
        privilegios = "ninguno relevante"

    return (
        "Eres el copiloto de una ambulancia en Arequipa, Peru. Con los datos de la ruta, "
        "escribe una recomendacion breve (maximo 2 oraciones, menos de 45 palabras) en espanol "
        "para quien conduce: que tramo vigilar y que privilegio de transito conviene usar. "
        "No inventes calles ni datos que no esten abajo. Sin saludos, sin listas, sin emojis.\n\n"
        f"Destino: {datos.destino}\n"
        f"Distancia: {km:.1f} km\n"
        f"ETA sin trafico: {minutos_libre:.0f} min; ETA con trafico real: {minutos_trafico:.0f} min\n"
        f"Avenidas de la ruta con trafico: {avenidas}\n"
        f"Privilegios de transito disponibles (contraflujo, carril exclusivo, cruce en rojo): {privilegios}\n"
        f"Riesgo de demora segun la app: {datos.riesgo_nivel} (puntaje {datos.riesgo_puntaje:.2f} de 1)"
    )


def _extraer_texto(respuesta: dict) -> str:
    try:
        partes = respuesta["candidates"][0]["content"]["parts"]
    except (KeyError, IndexError, TypeError) as e:
        raise IaNoDisponible("Gemini no devolvio texto") from e
    texto = " ".join(p["text"] for p in partes if "text" in p and not p.get("thought")).strip()
    if not texto:
        raise IaNoDisponible("Gemini devolvio una respuesta vacia")
    return texto


async def generar_recomendacion(datos: RecomendacionRequest) -> str:
    if not settings.gemini_api_key:
        raise IaNoDisponible("IA no configurada: falta GEMINI_API_KEY en backend/.env")

    clave = _clave_cache(datos)
    guardada = _cache.get(clave)
    if guardada and time.monotonic() - guardada[0] < CACHE_TTL_S:
        return guardada[1]

    cuerpo = {
        "contents": [{"parts": [{"text": construir_prompt(datos)}]}],
        "generationConfig": {"temperature": 0.4, "maxOutputTokens": 1024},
    }
    try:
        async with httpx.AsyncClient(timeout=15) as cliente:
            r = await cliente.post(
                GEMINI_URL.format(modelo=settings.gemini_model),
                headers={"x-goog-api-key": settings.gemini_api_key},
                json=cuerpo,
            )
            r.raise_for_status()
    except httpx.HTTPStatusError as e:
        raise IaNoDisponible(f"Gemini respondio {e.response.status_code}") from e
    except httpx.HTTPError as e:
        raise IaNoDisponible("No se pudo contactar a Gemini") from e

    texto = _extraer_texto(r.json())
    _cache[clave] = (time.monotonic(), texto)
    return texto
