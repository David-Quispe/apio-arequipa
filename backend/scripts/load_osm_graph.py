"""
Descarga el grafo vial (red 'drive') de OpenStreetMap para el area piloto de
Proyecto APIO (Arequipa Metropolitana) y lo carga en PostGIS como dos tablas:
osm_nodes y osm_edges.
"""

import json

from sqlalchemy import create_engine
import osmnx as ox

from app.core.config import settings

# Bounding box (left=west, bottom=south, right=east, top=north) en EPSG:4326.
# Ampliado el 2026-09-06 de "solo Cono Norte -> cluster hospitalario" a toda
# la extension urbana real de Arequipa Metropolitana (Cercado, Yanahuara,
# Cayma, Cerro Colorado, Jose Luis Bustamante y Rivero, Paucarpata, Mariano
# Melgar, Miraflores, Sachaca, Tiabaya, Uchumayo, Characato, Sabandia,
# Socabaya), a pedido del usuario -- cualquier origen dentro de la ciudad
# debe poder calcular ruta, no solo el Cono Norte. Mismo criterio que la
# ampliacion anterior (ver memoria del proyecto): se cubre la mancha urbana
# real, no el limite administrativo/provincial (que se mete en desierto
# despoblado hacia Yura/La Joya, nucleos separados por varios km sin
# poblacion). Los 4 hospitales destino y las 8 avenidas de la rubrica de
# privilegios NO cambian con esto -- es una ampliacion de cobertura de
# origen, no del corredor documentado.
CORRIDOR_BBOX = (-71.66, -16.47, -71.47, -16.33)


def _stringify_list_columns(gdf):
    """OSM puede devolver listas en columnas (ej. varios nombres de via);
    PostGIS/psycopg2 no acepta listas de Python como valor de columna, asi
    que las convertimos a JSON para no perder informacion."""
    for col in gdf.columns:
        if col == gdf.geometry.name:
            continue
        if gdf[col].apply(lambda v: isinstance(v, list)).any():
            gdf[col] = gdf[col].apply(lambda v: json.dumps(v) if isinstance(v, list) else v)
    return gdf


def main():
    print(f"Descargando red vial 'drive' en bbox {CORRIDOR_BBOX} ...")
    graph = ox.graph_from_bbox(bbox=CORRIDOR_BBOX, network_type="drive")
    print(f"Grafo descargado: {graph.number_of_nodes()} nodos, {graph.number_of_edges()} aristas")

    nodes_gdf, edges_gdf = ox.graph_to_gdfs(graph)

    nodes_gdf = _stringify_list_columns(nodes_gdf.reset_index())
    edges_gdf = _stringify_list_columns(edges_gdf.reset_index())

    engine = create_engine(settings.database_url)

    print("Cargando osm_nodes en PostGIS ...")
    nodes_gdf.to_postgis("osm_nodes", engine, if_exists="replace", index=False)

    print("Cargando osm_edges en PostGIS ...")
    edges_gdf.to_postgis("osm_edges", engine, if_exists="replace", index=False)

    print("Listo.")


if __name__ == "__main__":
    main()
