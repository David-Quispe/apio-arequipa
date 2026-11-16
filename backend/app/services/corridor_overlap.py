"""
Determina que avenidas cruza realmente una ruta calculada, para poder
aplicarle el factor de trafico de las avenidas correctas (no un promedio
generico) y para decirle al cliente por cuales avenidas pasa.

Hay DOS conjuntos de avenidas, a proposito separados:
- CASE_AVENIDA (8 avenidas): el corredor piloto original con rubrica de
  privilegios verificada en campo (separador_central/congestion_tipica,
  ver memoria del proyecto project_apio_rubrica). Solo estas tienen chips
  de privilegio ("Contraflujo 8.1/10", etc.) porque son las unicas con ese
  dato real -- agregar mas avenidas aca implicaria inventar valores de
  campo que nadie fue a verificar.
- CASE_AVENIDA_TRAFICO (esas 8 + otras avenidas principales de Arequipa,
  agregado 2026-09-06 a pedido del usuario al ampliar el area piloto a toda
  la ciudad): usada solo para trafico en vivo y el ajuste de ETA, sin
  pretender tener privilegios documentados. Elegidas viendo que avenidas
  cruzan de verdad rutas reales calculadas desde los nuevos distritos
  cubiertos (Paucarpata, Socabaya, Sachaca, Tiabaya, etc.), no adivinadas.
"""

from sqlalchemy import text

from app.core.db import engine

BUFFER_METROS = 15

# Reutilizado por app/services/privilegios.py y por el generador del custom
# model de GraphHopper (backend/scripts/generar_privilegios_graphhopper.py).
# Consulta contra `privilegios_via` (solo el corredor con rubrica de campo).
CASE_AVENIDA = """
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

# Consulta contra `osm_edges` (todo el grafo de Arequipa Metropolitana, no
# solo el corredor) -- por eso puede usar nombres mas amplios sin miedo a
# los choques de nombre que obligaron a filtrar por zona en
# compute_rubrica_privilegios.py: aca la ruta calculada (un buffer angosto
# de 15m alrededor de su propio trazado) ya es la restriccion geografica,
# asi que "Pizarro" solo puede matchear el tramo de Pizarro por el que esa
# ruta especifica realmente pasa, no un "Pizarro" homonimo en otro barrio.
CASE_AVENIDA_TRAFICO = """
    CASE
        WHEN name ILIKE '%j_rcito%' THEN 'Av. Ejercito'
        WHEN name ILIKE '%puente grau%' THEN 'Puente Grau'
        WHEN name ILIKE '%bolognesi%' THEN 'Av. Bolognesi'
        WHEN name ILIKE '%dolores%' THEN 'Av. Dolores'
        WHEN name ILIKE '%carri_n%' THEN 'Av. Alcides Carrion'
        WHEN name ILIKE '%goyeneche%' THEN 'Av. Goyeneche'
        WHEN name ILIKE '%peral%' THEN 'Peral'
        WHEN name ILIKE '%filtro%' THEN 'El Filtro'
        WHEN name ILIKE '%lambramani%' THEN 'Av. Lambramani'
        WHEN name ILIKE '%pizarro%' THEN 'Av. Pizarro'
        WHEN name ILIKE '%fernandini%' THEN 'Av. Fernandini'
        WHEN name ILIKE '%bela_nde%' THEN 'Av. Victor Andres Belaunde'
        WHEN name ILIKE '%qui_ones%' THEN 'Av. Jose Abelardo Quinones'
        WHEN name ILIKE '%alfonso ugarte%' THEN 'Av. Alfonso Ugarte'
        WHEN name ILIKE '%arancota%' THEN 'Av. Arancota'
        WHEN name ILIKE '%alameda de la cultura%' THEN 'Av. Alameda de la Cultura'
    END
"""


def _avenidas_en_ruta(coordenadas_lonlat: list[list[float]], tabla: str, case_avenida: str) -> dict[str, float]:
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
                SELECT {case_avenida} AS avenida,
                       SUM(ST_Length(ST_Intersection(geometry, ruta.buf)::geography)) AS metros
                FROM {tabla}, ruta
                WHERE ST_Intersects(geometry, ruta.buf)
                GROUP BY avenida
                """
            ),
            {"wkt": wkt, "buffer": BUFFER_METROS},
        ).fetchall()

    return {r.avenida: float(r.metros) for r in rows if r.avenida is not None and r.metros}


def avenidas_en_ruta(coordenadas_lonlat: list[list[float]]) -> dict[str, float]:
    """Devuelve {avenida: metros_superpuestos} para las 8 avenidas del
    corredor piloto (con rubrica de privilegios) que la ruta realmente
    atraviesa (dentro de un buffer de 15m). Usado para los chips de
    privilegio -- no ampliar esta lista sin agregar tambien la verificacion
    de campo correspondiente en privilegios_via."""
    return _avenidas_en_ruta(coordenadas_lonlat, "privilegios_via", CASE_AVENIDA)


def avenidas_trafico_en_ruta(coordenadas_lonlat: list[list[float]]) -> dict[str, float]:
    """Igual que avenidas_en_ruta pero contra TODO el grafo vial (osm_edges)
    y con la lista ampliada de avenidas (CASE_AVENIDA_TRAFICO) -- usado para
    el ajuste de ETA por trafico y para el panel de trafico del cliente,
    donde no hace falta tener privilegios documentados."""
    return _avenidas_en_ruta(coordenadas_lonlat, "osm_edges", CASE_AVENIDA_TRAFICO)
