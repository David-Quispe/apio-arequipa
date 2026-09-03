"""
Fase 1 - Rubrica de clasificacion de privilegios (parte automatizable).

Selecciona los edges de osm_edges que forman el corredor piloto ya
documentado (ver memoria del proyecto / README) y calcula sobre ellos el
score automatizable de la rubrica.

Un segmento entra al corredor si:
  1. Su nombre coincide con una de las avenidas del corredor, Y
  2. Cae dentro de la "zona del corredor" (envolvente convexa de los puntos
     de origen en el Cono Norte + los 4 hospitales, con margen).
La condicion (2) es necesaria porque varios nombres de calle se repiten en
otras partes de Arequipa (ej. "Miguel Grau" vs. "Puente Grau", o "Francisco
Bolognesi" en Cerro Colorado vs. en Yanahuara) y el nombre solo no alcanza
para identificar el tramo correcto.

Criterios automatizables:
  - Jerarquia vial (OSM highway)
  - Carriles / ancho de via
  - Semaforos por km (nodos OSM highway=traffic_signals cerca del edge)

Quedan pendientes para la etapa de verificacion en campo (no se calculan
aqui): separador central y nivel de congestion tipica.
"""

import json

import geopandas as gpd
import pandas as pd
from shapely.geometry import MultiPoint
from sqlalchemy import create_engine, text

from app.core.config import settings

UTM_AREQUIPA = "EPSG:32719"  # WGS84 / UTM zone 19S
MARGEN_ZONA_M = 700

# (lat, lon) de los puntos que delimitan la zona del corredor: origenes en
# el Cono Norte + los 4 hospitales/clinicas del corredor piloto.
PUNTOS_ANCLA = [
    (-16.3772567, -71.5581999),  # Cerro Colorado
    (-16.3833, -71.5500),  # Cayma
    (-16.3908, -71.5478),  # Yanahuara
    (-16.4149129, -71.5315582),  # Hospital Honorio Delgado Espinoza
    (-16.4024925, -71.5279176),  # Hospital Goyeneche
    (-16.3952624, -71.5307408),  # Hospital Seguín Escobedo
    (-16.391834, -71.5400938),  # Clínica Arequipa
]

# Patrones ILIKE de las avenidas del corredor (ver README / memoria del proyecto)
PATRONES_AVENIDAS = {
    "Av. Ejército": "%j_rcito%",
    "Puente Grau": "%puente grau%",
    "Av. Bolognesi": "%bolognesi%",
    "Av. Dolores": "%dolores%",
    "Av. Daniel Alcides Carrión": "%carri_n%",
    "Av. Goyeneche": "%goyeneche%",
    "Peral": "%peral%",
    "El Filtro": "%filtro%",
}


def construir_zona_corredor():
    puntos = MultiPoint([(lon, lat) for lat, lon in PUNTOS_ANCLA])
    zona_utm = gpd.GeoSeries([puntos], crs="EPSG:4326").to_crs(UTM_AREQUIPA).iloc[0]
    zona_utm = zona_utm.convex_hull.buffer(MARGEN_ZONA_M)
    return gpd.GeoSeries([zona_utm], crs=UTM_AREQUIPA).to_crs("EPSG:4326").iloc[0]


def score_jerarquia(highway):
    if pd.isna(highway) or not highway:
        return None
    valores = json.loads(highway) if highway.startswith("[") else [highway]
    ranking = {
        "trunk": 10, "trunk_link": 9,
        "primary": 9, "primary_link": 8,
        "secondary": 7, "secondary_link": 6,
        "tertiary": 5, "tertiary_link": 4,
        "unclassified": 3, "residential": 3,
        "living_street": 1, "service": 1,
    }
    return max((ranking.get(v, 2) for v in valores), default=None)


def score_carriles(lanes, width):
    score_lanes = None
    if not pd.isna(lanes) and lanes:
        try:
            n = int(json.loads(lanes)[0]) if lanes.startswith("[") else int(lanes)
            score_lanes = {1: 2, 2: 5}.get(n, 7 if n == 3 else 10)
        except (ValueError, TypeError):
            pass

    score_width = None
    if not pd.isna(width) and width:
        try:
            w = float(json.loads(width)[0]) if width.startswith("[") else float(width)
            score_width = 10 if w >= 14 else 7 if w >= 10 else 5 if w >= 7 else 2
        except (ValueError, TypeError):
            pass

    valores = [v for v in (score_lanes, score_width) if v is not None]
    return round(sum(valores) / len(valores), 1) if valores else None


def score_semaforos_por_km(n_semaforos, longitud_m):
    if longitud_m <= 0:
        return None
    por_km = n_semaforos / (longitud_m / 1000)
    if por_km == 0:
        return 2
    if por_km <= 3:
        return 6
    return 10


def main():
    zona_corredor = construir_zona_corredor()
    engine = create_engine(settings.database_url)

    patrones = list(PATRONES_AVENIDAS.values())
    params = {f"p{i}": patron for i, patron in enumerate(patrones)}
    params["zona_wkt"] = zona_corredor.wkt
    condiciones = " OR ".join(f"name ILIKE :p{i}" for i in range(len(patrones)))

    print("Seleccionando edges por nombre + zona del corredor...")
    edges = gpd.read_postgis(
        text(
            "SELECT u, v, key, osmid, name, highway, lanes, width, length, geometry "
            "FROM osm_edges "
            f"WHERE ({condiciones}) "
            "AND ST_Intersects(geometry, ST_GeomFromText(:zona_wkt, 4326))"
        ),
        engine,
        params=params,
        geom_col="geometry",
    )
    print(f"Edges del corredor: {len(edges)} (longitud total: {edges['length'].sum() / 1000:.2f} km)")

    print("Contando semaforos cercanos a cada edge...")
    edges_utm = edges.to_crs(UTM_AREQUIPA)
    semaforos = gpd.read_postgis(
        "SELECT osmid, geometry FROM osm_nodes WHERE highway = 'traffic_signals'",
        engine,
        geom_col="geometry",
    ).to_crs(UTM_AREQUIPA)

    edges_utm["n_semaforos"] = edges_utm.geometry.buffer(20).apply(
        lambda buf: semaforos.geometry.within(buf).sum()
    )

    edges["score_jerarquia"] = edges["highway"].apply(score_jerarquia)
    edges["score_carriles"] = edges.apply(lambda r: score_carriles(r["lanes"], r["width"]), axis=1)
    edges["n_semaforos"] = edges_utm["n_semaforos"]
    edges["score_semaforos"] = edges.apply(
        lambda r: score_semaforos_por_km(r["n_semaforos"], r["length"]), axis=1
    )

    componentes = edges[["score_jerarquia", "score_carriles", "score_semaforos"]]
    edges["score_final"] = componentes.mean(axis=1, skipna=True).round(1)
    edges["datos_incompletos"] = componentes.isna().any(axis=1)

    # Placeholders para la parte manual de la rubrica (verificacion en campo)
    edges["separador_central"] = None
    edges["congestion_tipica"] = None

    print("Guardando en la tabla privilegios_via ...")
    edges.to_postgis("privilegios_via", engine, if_exists="replace", index=False)

    print("\nResumen por avenida (segun patron de nombre):")
    for etiqueta, patron in PATRONES_AVENIDAS.items():
        regex = patron.strip("%").replace("_", ".")
        subset = edges[edges["name"].str.contains(regex, case=False, na=False, regex=True)]
        if len(subset) == 0:
            print(f"  {etiqueta}: SIN SEGMENTOS (revisar)")
            continue
        print(
            f"  {etiqueta}: {len(subset)} segmentos, "
            f"{subset['length'].sum():.0f} m, score promedio {subset['score_final'].mean():.1f}"
        )

    print(f"\nSegmentos con datos incompletos: {edges['datos_incompletos'].sum()} / {len(edges)}")
    print("Listo.")


if __name__ == "__main__":
    main()
