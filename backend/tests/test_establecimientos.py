import json

from fastapi.testclient import TestClient

from app.main import app
from app.services import establecimientos as servicio
from app.services.establecimientos import (
    listar_establecimientos,
    normalizar_texto,
    tiene_emergencia,
    tipo_desde_institucion,
)

client = TestClient(app)


def test_emergencia_por_categoria_y_por_clasificacion():
    assert tiene_emergencia("HOSPITALES O CLINICAS DE ATENCION GENERAL", "III-1")
    assert tiene_emergencia("HOSPITALES O CLINICAS DE ATENCION GENERAL", None)  # clinica privada sin categoria
    assert tiene_emergencia("CENTROS DE SALUD O CENTROS MEDICOS", "II-1")
    assert not tiene_emergencia("PUESTOS DE SALUD O POSTAS DE SALUD", "I-1")
    assert not tiene_emergencia("CENTROS DE SALUD CON CAMAS DE INTERNAMIENTO", "I-4")


def test_tipo_desde_institucion():
    assert tipo_desde_institucion("GOBIERNO REGIONAL") == "MINSA"
    assert tipo_desde_institucion("ESSALUD") == "EsSalud"
    assert tipo_desde_institucion("PRIVADO") == "Privada"
    assert tipo_desde_institucion("SANIDAD DE LA POLICIA NACIONAL DEL PERU") == "FFAA/PNP"


def test_normalizar_quita_tildes_y_espacios():
    assert normalizar_texto("  Clínica   Arequipa ") == "CLINICA AREQUIPA"


def test_filtra_solo_emergencia(tmp_path):
    archivo = tmp_path / "est.json"
    archivo.write_text(
        json.dumps({
            "establecimientos": [
                {"nombre": "Hospital A", "emergencia": True, "lat": -16.4, "lon": -71.5},
                {"nombre": "Puesto B", "emergencia": False, "lat": -16.4, "lon": -71.5},
            ]
        }),
        encoding="utf-8",
    )
    servicio.cargar_establecimientos.cache_clear()
    assert len(listar_establecimientos(ruta=str(archivo))) == 2
    assert [e["nombre"] for e in listar_establecimientos(True, ruta=str(archivo))] == ["Hospital A"]
    servicio.cargar_establecimientos.cache_clear()


def test_endpoint_responde(monkeypatch):
    monkeypatch.setattr(
        "app.api.routes.establecimientos.listar_establecimientos",
        lambda solo_emergencia=False: [{"nombre": "Hospital A", "emergencia": True}],
    )
    r = client.get("/api/establecimientos?solo_emergencia=true")
    assert r.status_code == 200
    assert r.json()["total"] == 1
