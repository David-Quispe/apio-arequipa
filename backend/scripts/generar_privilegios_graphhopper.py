"""
Fase 1 (cierre) - conecta la rubrica de privilegios al motor de ruteo real.

Genera routing/apio_car.json completo a partir de privilegios_via en PostGIS,
usando el mecanismo nativo de "areas" de GraphHopper para custom models
(https://github.com/graphhopper/graphhopper/blob/master/docs/core/custom-models.md):
se definen poligonos con un id, y las reglas de prioridad pueden usar
"in_<id>" como condicion booleana. Esto evita tener que enlazar IDs de edge
entre el grafo propio de OSMnx (usado para la rubrica) y el grafo interno de
GraphHopper (importado por separado desde el mismo extracto OSM) -- alcanza
con que el poligono cubra la via, sin importar si los IDs coinciden.

Cada avenida del corredor recibe un plus de prioridad proporcional a su
score_con_campo (jerarquia + carriles + semaforos + separador central +
congestion observada, ver actualizar_rubrica_campo.py), ademas de las reglas
generales de jerarquia/carriles que ya existian.

Sobrescribe routing/apio_car.json entero (no editar ese archivo a mano).
Despues de correrlo hay que borrar routing/data/graph-cache y reiniciar
GraphHopper para que reconstruya el grafo (los perfiles CH "hornean" el
custom model en el grafo, asi que un cambio no se aplica en caliente).
"""

import json
from pathlib import Path

import geopandas as gpd
from shapely.ops import unary_union
from sqlalchemy import create_engine, text

from app.core.config import settings
from app.services.corridor_overlap import CASE_AVENIDA

UTM_AREQUIPA = "EPSG:32719"  # WGS84 / UTM zone 19S
BUFFER_M = 30  # cubre ancho de via + pequenas diferencias entre los 2 grafos

AVENIDA_SLUGS = {
    "Av. Ejercito": "av_ejercito",
    "Puente Grau": "puente_grau",
    "Av. Bolognesi": "av_bolognesi",
    "Av. Dolores": "av_dolores",
    "Av. Alcides Carrion": "av_alcides_carrion",
    "Av. Goyeneche": "av_goyeneche",
    "Peral": "peral",
    "El Filtro": "el_filtro",
}

ROUTING_DIR = Path(__file__).resolve().parents[2] / "routing"


def boost_desde_score(score: float) -> float:
    """Score 0-10 de viabilidad de privilegio -> multiplicador de prioridad.
    Rango de 0.4: una avenida con score 10 se prefiere ~40% mas que una con
    score 0, encima de las reglas de jerarquia/carriles ya existentes."""
    return round(1.0 + (score / 10) * 0.4, 2)


def main():
    engine = create_engine(settings.database_url)

    print("Leyendo privilegios_via agrupado por avenida...")
    edges = gpd.read_postgis(
        text(f"SELECT {CASE_AVENIDA} AS avenida, score_con_campo, geometry FROM privilegios_via"),
        engine,
        geom_col="geometry",
    )
    edges = edges[edges["avenida"].notna()]

    features = []
    reglas_prioridad = []

    for avenida, grupo in edges.groupby("avenida"):
        slug = AVENIDA_SLUGS[avenida]
        score = grupo["score_con_campo"].mean()
        boost = boost_desde_score(score)

        geom_utm = gpd.GeoSeries(grupo.geometry.values, crs="EPSG:4326").to_crs(UTM_AREQUIPA)
        area_utm = unary_union(geom_utm.buffer(BUFFER_M))
        area_wgs84 = gpd.GeoSeries([area_utm], crs=UTM_AREQUIPA).to_crs("EPSG:4326").iloc[0]

        partes = list(area_wgs84.geoms) if area_wgs84.geom_type == "MultiPolygon" else [area_wgs84]
        for i, parte in enumerate(partes):
            area_id = slug if len(partes) == 1 else f"{slug}_{i + 1}"
            geometria = json.loads(gpd.GeoSeries([parte], crs="EPSG:4326").to_json())["features"][0]["geometry"]
            features.append({
                "type": "Feature",
                "id": area_id,
                "properties": {"avenida": avenida, "score_con_campo": round(score, 1)},
                "geometry": geometria,
            })
            reglas_prioridad.append({"if": f"in_{area_id}", "multiply_by": str(boost)})

        print(f"  {avenida}: score={score:.1f} -> boost={boost} ({len(partes)} poligono(s))")

    custom_model = {
        "distance_influence": 90,
        "priority": [
            {"if": "!car_access", "multiply_by": "0"},
            {"if": "road_class == RESIDENTIAL", "multiply_by": "0.6"},
            {"if": "road_class == LIVING_STREET", "multiply_by": "0.3"},
            {"if": "road_class == SERVICE", "multiply_by": "0.3"},
            {"if": "road_class == TRACK", "multiply_by": "0.1"},
            {"if": "lanes >= 3", "multiply_by": "1.15"},
            {"else_if": "lanes == 1", "multiply_by": "0.85"},
            *reglas_prioridad,
        ],
        "speed": [
            {"if": "true", "limit_to": "car_average_speed"},
        ],
        "areas": {
            "type": "FeatureCollection",
            "features": features,
        },
    }

    destino = ROUTING_DIR / "apio_car.json"
    with open(destino, "w", encoding="utf-8") as f:
        f.write(
            "// Modelo de costos de Proyecto APIO. GENERADO por\n"
            "// backend/scripts/generar_privilegios_graphhopper.py -- no editar a mano,\n"
            "// volver a correr el script si cambia privilegios_via.\n"
            "//\n"
            "// Ademas del filtro base de car_access y las reglas de jerarquia/carriles\n"
            "// (calles residenciales/de servicio penalizadas, mas carriles premiados,\n"
            "// aplicadas a toda la red), las reglas 'in_<avenida>' del final le dan un\n"
            "// plus de prioridad a las avenidas del corredor piloto proporcional a su\n"
            "// score_con_campo de la rubrica de privilegios -- la ruta elegida ahora sí\n"
            "// refleja donde es mas viable ejercer el privilegio de via legal, no solo\n"
            "// la jerarquia vial generica.\n"
        )
        json.dump(custom_model, f, ensure_ascii=False, indent=2)
        f.write("\n")

    print(f"\nEscrito {destino} con {len(features)} area(s) de privilegio.")
    print("Ahora: borrar routing/data/graph-cache y reiniciar GraphHopper.")


if __name__ == "__main__":
    main()
