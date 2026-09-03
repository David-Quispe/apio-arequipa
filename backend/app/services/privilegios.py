"""
Scores de la rubrica de privilegios (tabla privilegios_via en PostGIS, ver
memoria del proyecto project_apio_rubrica), agregados por avenida para
mostrarlos junto al ETA en /api/route.
"""

from sqlalchemy import text

from app.core.db import engine
from app.services.corridor_overlap import CASE_AVENIDA


def scores_por_avenida() -> dict[str, float]:
    with engine.connect() as conn:
        rows = conn.execute(
            text(f"SELECT {CASE_AVENIDA} AS avenida, AVG(score_con_campo) AS score FROM privilegios_via GROUP BY avenida")
        ).fetchall()
    return {r.avenida: round(float(r.score), 1) for r in rows if r.avenida is not None and r.score is not None}
