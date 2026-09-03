"""
Determina que avenidas del corredor piloto cruza realmente una ruta calculada,
para poder aplicarle el factor de trafico de las avenidas correctas (no un
promedio generico del corredor completo).
"""

from sqlalchemy import text

from app.core.db import engine

BUFFER_METROS = 15

_CASE_AVENIDA = """
    CASE
        WHEN name ILIKE '%j_rcito%' THEN 'Av. Ejercito'
        WHEN name ILIKE '%puente grau%' THEN 'Puente Grau'
        WHEN name ILIKE '%bolognesi%' THEN 'Av. Bolognesi'
        WHEN name ILIKE '%dolores%' THEN 'Av. Dolores'
        WHEN name ILIKE '%carri_n%' THEN 'Av. Alcides Carrion'
        WHEN name ILIKE '%goyeneche%' THEN 'Av. Goyeneche'
        WHEN name ILIKE '%peral%' THEN 'Peral'
        WHEN name ILIKE '%filtro%' THEN 'El Filtro'
    END
"""


def avenidas_en_ruta(coordenadas_lonlat: list[list[float]]) -> dict[str, float]:
    """Devuelve {avenida: metros_superpuestos} para las avenidas del corredor
    que la ruta realmente atraviesa (dentro de un buffer de 15m)."""
    if len(coordenadas_lonlat) < 2:
        return {}

    wkt = "LINESTRING(" + ", ".join(f"{lon} {lat}" for lon, lat in coordenadas_lonlat) + ")"

    with engine.connect() as conn:
        rows = conn.execute(
            text(
                f"""
                WITH ruta AS (
                    SELECT ST_Buffer(ST_GeomFromText(:wkt, 4326)::geography, :buffer)::geometry AS buf
                )
                SELECT {_CASE_AVENIDA} AS avenida,
                       SUM(ST_Length(ST_Intersection(geometry, ruta.buf)::geography)) AS metros
                FROM privilegios_via, ruta
                WHERE ST_Intersects(geometry, ruta.buf)
                GROUP BY avenida
                """
            ),
            {"wkt": wkt, "buffer": BUFFER_METROS},
        ).fetchall()

    return {r.avenida: float(r.metros) for r in rows if r.avenida is not None and r.metros}
