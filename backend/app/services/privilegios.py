"""
Scores de la rubrica de privilegios (tabla privilegios_via en PostGIS, ver
memoria del proyecto project_apio_rubrica), agregados por avenida para
mostrarlos junto al ETA en /api/route.
"""

from sqlalchemy import text

from app.core.db import engine
from app.services.corridor_overlap import CASE_AVENIDA

# Nombres de los 3 privilegios legales especificos (ver
# clasificar_privilegios_especificos.py) para no repetir strings sueltos.
TIPOS_PRIVILEGIO = {
    "contraflujo": "Contraflujo",
    "carril_exclusivo": "Carril exclusivo",
    "cruce_rojo": "Cruce de semáforo en rojo",
}


def scores_por_avenida() -> dict[str, float]:
    with engine.connect() as conn:
        rows = conn.execute(
            text(f"SELECT {CASE_AVENIDA} AS avenida, AVG(score_con_campo) AS score FROM privilegios_via GROUP BY avenida")
        ).fetchall()
    return {r.avenida: round(float(r.score), 1) for r in rows if r.avenida is not None and r.score is not None}


def detalle_privilegios_por_avenida() -> dict[str, dict]:
    """Para cada avenida: el puntaje general (score_con_campo, el que alimenta
    el boost de prioridad en GraphHopper) y el puntaje de cada uno de los 3
    privilegios especificos, mas cual de esos 3 es el mas viable ahi."""
    with engine.connect() as conn:
        rows = conn.execute(
            text(
                f"""
                SELECT {CASE_AVENIDA} AS avenida,
                       AVG(score_con_campo) AS general,
                       AVG(score_contraflujo) AS contraflujo,
                       AVG(score_carril_exclusivo) AS carril_exclusivo,
                       AVG(score_cruce_rojo) AS cruce_rojo
                FROM privilegios_via
                GROUP BY avenida
                """
            )
        ).fetchall()

    resultado = {}
    for r in rows:
        if r.avenida is None or r.general is None:
            continue
        especificos = {
            "contraflujo": r.contraflujo,
            "carril_exclusivo": r.carril_exclusivo,
            "cruce_rojo": r.cruce_rojo,
        }
        especificos = {k: round(float(v), 1) for k, v in especificos.items() if v is not None}
        clave_principal = max(especificos, key=especificos.get) if especificos else None
        resultado[r.avenida] = {
            "general": round(float(r.general), 1),
            "especificos": especificos,
            "tipo_principal": TIPOS_PRIVILEGIO.get(clave_principal),
            "tipo_principal_score": especificos.get(clave_principal),
        }
    return resultado
