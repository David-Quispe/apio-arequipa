"""
Descarga el grafo vial (red 'drive') de OpenStreetMap para el corredor piloto
de Proyecto APIO (Cono Norte -> cluster hospitalario del Cercado de Arequipa)
y lo carga en PostGIS como dos tablas: osm_nodes y osm_edges.
"""

import json

from sqlalchemy import create_engine
import osmnx as ox

from app.core.config import settings

# Bounding box (left=west, bottom=south, right=east, top=north) en EPSG:4326.
# Cubre el Cono Norte real (Cerro Colorado incluyendo Peruarbo, Rio Seco y
# Zamacola -- no solo Cayma/Yanahuara) hasta el cluster hospitalario del
# Cercado (destino), incluyendo Puente Grau. Ampliado el 2026-09-03: el bbox
# original dejaba fuera zonas de expansion real del Cono Norte (ver memoria
# del proyecto), que es justamente la poblacion que motiva el proyecto.
CORRIDOR_BBOX = (-71.63, -16.42, -71.52, -16.33)


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
