"""
Precalcula las rutas de la "demo semi-viva" (deploy estatico + funciones
serverless en Vercel, ver README) para un set fijo de origenes del Cono Norte
y los 4 hospitales del corredor.

La demo no tiene GraphHopper/PostGIS corriendo detras -- por eso el camino,
la distancia y los metros que cada ruta superpone con cada avenida del
corredor (para poder ponderar el trafico igual que backend/app/api/routes/
route.py) se calculan UNA VEZ aca, contra el GraphHopper/PostGIS reales, y se
guardan como JSON. La funcion serverless de la demo (frontend/api/route.js)
solo le suma el tráfico EN VIVO de TomTom a estos numeros ya calculados.

Solo la distancia/el camino quedan fijos; el ETA sigue siendo dinamico.
"""

import json
from pathlib import Path

import httpx

from app.core.config import settings
from app.services.corridor_overlap import avenidas_en_ruta
from app.services.privilegios import scores_por_avenida

ORIGENES = [
    {"id": "cerro_colorado", "nombre": "Cerro Colorado", "posicion": [-16.3772567, -71.5581999]},
    {"id": "cayma", "nombre": "Cayma", "posicion": [-16.3833, -71.5500]},
    {"id": "yanahuara", "nombre": "Yanahuara", "posicion": [-16.3908, -71.5478]},
    {"id": "peruarbo", "nombre": "Peruarbo", "posicion": [-16.3476582, -71.5965792]},
]

DESTINOS = [
    {
        "id": "honorio_delgado",
        "nombre": "Hospital Regional Honorio Delgado Espinoza",
        "tipo": "MINSA",
        "posicion": [-16.4149129, -71.5315582],
    },
    {
        "id": "goyeneche",
        "nombre": "Hospital Goyeneche",
        "tipo": "MINSA",
        "posicion": [-16.4024925, -71.5279176],
    },
    {
        "id": "seguin_escobedo",
        "nombre": "Hospital Nacional Carlos Alberto Seguín Escobedo",
        "tipo": "EsSalud",
        "posicion": [-16.3952624, -71.5307408],
    },
    {
        "id": "clinica_arequipa",
        "nombre": "Clínica Arequipa",
        "tipo": "Privada",
        "posicion": [-16.391834, -71.5400938],
    },
]

SALIDA = Path(__file__).resolve().parents[2] / "frontend" / "src" / "demo" / "rutas.json"


def calcular_ruta(origen, destino):
    params = {
        "point": [f"{origen[0]},{origen[1]}", f"{destino[0]},{destino[1]}"],
        "profile": "car",
        "points_encoded": "false",
    }
    resp = httpx.get(f"{settings.graphhopper_url}/route", params=params, timeout=30)
    resp.raise_for_status()
    return resp.json()["paths"][0]


def main():
    scores = scores_por_avenida()
    rutas = {}

    for origen in ORIGENES:
        for destino in DESTINOS:
            clave = f"{origen['id']}|{destino['id']}"
            print(f"Calculando {clave}...")
            path = calcular_ruta(origen["posicion"], destino["posicion"])
            metros_por_avenida = avenidas_en_ruta(path["points"]["coordinates"])
            privilegios_cruzados = sorted(
                (
                    {"avenida": avenida, "score": scores[avenida]}
                    for avenida in metros_por_avenida
                    if avenida in scores
                ),
                key=lambda p: -p["score"],
            )
            rutas[clave] = {
                "distance_m": path["distance"],
                "time_s": path["time"] / 1000,
                "geometry": path["points"],
                "metros_por_avenida": metros_por_avenida,
                "privilegios_cruzados": privilegios_cruzados,
            }

    SALIDA.parent.mkdir(parents=True, exist_ok=True)
    with open(SALIDA, "w", encoding="utf-8") as f:
        json.dump({"origenes": ORIGENES, "destinos": DESTINOS, "rutas": rutas}, f, ensure_ascii=False, indent=2)

    print(f"\nEscrito {SALIDA} con {len(rutas)} rutas precalculadas.")


if __name__ == "__main__":
    main()
