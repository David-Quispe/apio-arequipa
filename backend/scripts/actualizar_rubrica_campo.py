"""
Fase 1 - Rubrica de privilegios: parte manual (verificacion de campo).

Llena separador_central y congestion_tipica en privilegios_via a partir de
inspeccion visual (satelital + trafico en vivo de Google Maps) hecha el
2026-09-03 - ver memoria del proyecto (project_apio_rubrica.md) para el
detalle de la observacion en cada tramo.

Nota: es una primera pasada, criterio de una sola observacion (no repetida
en distintos horarios). Sirve como punto de partida, no como medicion
definitiva de congestion (eso lo resuelve mejor la Fase 3 con datos de
trafico en tiempo real).
"""

from sqlalchemy import create_engine, text

from app.core.config import settings
from app.services.corridor_overlap import CASE_AVENIDA

# (patron ILIKE, separador_central, congestion_tipica)
OBSERVACIONES = [
    ("%j_rcito%", "fisico", "alta"),
    ("%puente grau%", "ninguno", "media"),
    ("%bolognesi%", "ninguno", "baja"),
    ("%dolores%", "ninguno", "media"),
    ("%carri_n%", "ninguno", "media"),
    ("%goyeneche%", "ninguno", "media"),
    ("%peral%", "ninguno", "baja"),
    ("%filtro%", "ninguno", "baja"),
]

# Score numerico para las 2 columnas manuales, mismo estilo 0-10 que las
# automatizables (10 = mas viable para ejercer el privilegio con seguridad)
SCORE_SEPARADOR = {"fisico": 10, "linea_pintada": 6, "ninguno": 3}
SCORE_CONGESTION = {"baja": 10, "media": 6, "alta": 2}


def recalcular_score_con_campo(conn):
    """Recalcula score_con_campo (promedio de los 5 criterios de la rubrica:
    los 3 automatizables + separador_central + congestion_tipica, ignorando
    los que falten). Se reusa desde aca y desde
    actualizar_congestion_desde_historico.py -- una sola formula, un solo
    lugar."""
    conn.execute(text("ALTER TABLE privilegios_via ADD COLUMN IF NOT EXISTS score_con_campo double precision"))
    conn.execute(
        text(
            """
            UPDATE privilegios_via SET score_con_campo = sub.promedio
            FROM (
                SELECT
                    ctid,
                    (
                        COALESCE(score_jerarquia, 0) * (score_jerarquia IS NOT NULL)::int +
                        COALESCE(score_carriles, 0) * (score_carriles IS NOT NULL)::int +
                        COALESCE(score_semaforos, 0) * (score_semaforos IS NOT NULL)::int +
                        COALESCE(
                            CASE separador_central
                                WHEN 'fisico' THEN 10 WHEN 'linea_pintada' THEN 6 WHEN 'ninguno' THEN 3
                            END, 0) *
                        (separador_central IS NOT NULL)::int +
                        COALESCE(
                            CASE congestion_tipica
                                WHEN 'baja' THEN 10 WHEN 'media' THEN 6 WHEN 'alta' THEN 2
                            END, 0) *
                        (congestion_tipica IS NOT NULL)::int
                    ) / NULLIF(
                        (score_jerarquia IS NOT NULL)::int +
                        (score_carriles IS NOT NULL)::int +
                        (score_semaforos IS NOT NULL)::int +
                        (separador_central IS NOT NULL)::int +
                        (congestion_tipica IS NOT NULL)::int
                    , 0) AS promedio
                FROM privilegios_via
            ) AS sub
            WHERE privilegios_via.ctid = sub.ctid
            """
        )
    )


def imprimir_resumen(engine):
    print("\nResumen final por avenida (score_con_campo):")
    with engine.connect() as conn:
        rows = conn.execute(
            text(
                f"""
                SELECT {CASE_AVENIDA} AS avenida,
                    round(avg(score_final)::numeric, 1) AS score_automatizable,
                    round(avg(score_con_campo)::numeric, 1) AS score_con_campo
                FROM privilegios_via
                GROUP BY avenida ORDER BY score_con_campo DESC
                """
            )
        ).fetchall()
        for r in rows:
            print(f"  {r.avenida}: automatizable={r.score_automatizable} -> con_campo={r.score_con_campo}")


def main():
    engine = create_engine(settings.database_url)
    with engine.begin() as conn:
        for patron, separador, congestion in OBSERVACIONES:
            result = conn.execute(
                text(
                    "UPDATE privilegios_via "
                    "SET separador_central = :separador, congestion_tipica = :congestion "
                    "WHERE name ILIKE :patron"
                ),
                {"separador": separador, "congestion": congestion, "patron": patron},
            )
            print(f"{patron}: {result.rowcount} segmentos actualizados "
                  f"(separador={separador}, congestion={congestion})")

        # Score final actualizado con los 5 criterios (los 3 automatizables +
        # los 2 de campo). Se guarda aparte de score_final para no perder
        # trazabilidad de cual score corresponde a que metodologia.
        recalcular_score_con_campo(conn)

    imprimir_resumen(engine)
    print("\nListo.")


if __name__ == "__main__":
    main()
