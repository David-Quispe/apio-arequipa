from fastapi.testclient import TestClient

from app.core.config import settings
from app.main import app
from app.services import ia
from app.services.ia import RecomendacionRequest, construir_prompt

client = TestClient(app)

DATOS = {
    "destino": "Hospital Honorio Delgado",
    "distancia_m": 7400,
    "tiempo_s": 720,
    "tiempo_s_con_trafico": 1020,
    "avenidas_trafico": [{"avenida": "Av. Ejercito", "nivel": "alta"}],
    "privilegios": [{"avenida": "Av. Ejercito", "tipo": "carril_exclusivo", "score": 8.2}],
    "riesgo_nivel": "MEDIO",
    "riesgo_puntaje": 0.48,
}


def test_sin_key_responde_503(monkeypatch):
    monkeypatch.setattr(settings, "gemini_api_key", "")
    r = client.post("/api/ia/recomendacion", json=DATOS)
    assert r.status_code == 503


def test_prompt_incluye_datos_reales():
    prompt = construir_prompt(RecomendacionRequest(**DATOS))
    assert "Av. Ejercito (trafico alta)" in prompt
    assert "17 min" in prompt
    assert "MEDIO" in prompt


def test_con_key_devuelve_texto_de_gemini(monkeypatch):
    monkeypatch.setattr(settings, "gemini_api_key", "key-de-prueba")
    ia._cache.clear()

    async def falso(_datos):
        return "Evita la Av. Ejercito y usa el carril exclusivo."

    monkeypatch.setattr("app.api.routes.ia.generar_recomendacion", falso)
    r = client.post("/api/ia/recomendacion", json=DATOS)
    assert r.status_code == 200
    assert r.json()["texto"].startswith("Evita")
