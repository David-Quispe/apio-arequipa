"""
Extiende la rubrica de privilegios (Fase 1) para distinguir CUAL de los 3
privilegios legales de vehiculo de emergencia (contraflujo, carril exclusivo,
cruce de semaforo en rojo) es el aplicable en cada tramo, en vez de un solo
puntaje compuesto de "viabilidad general" (score_con_campo).

El Reglamento Nacional de Transito (D.S. 016-2009-MTC) otorga el privilegio
de forma general (toda la red, condicionado a que se ejerza con seguridad),
sin distinguir tramo por tramo -- esa distincion es un juicio de ingenieria
sobre viabilidad fisica/operativa, no una clasificacion legal, y se basa en
los mismos datos ya recolectados para la rubrica:

- Cruce de semaforo en rojo: se ejerce en los cruces semaforizados. Ya existe
  un criterio para esto (score_semaforos, semaforos por km) -- se reusa tal
  cual, con otro nombre.
- Carril exclusivo: no hay carriles de bus/emergencia ya designados en OSM
  para este corredor (las columnas descargadas de OSMnx ni siquiera incluyen
  tags como busway/lanes:psv). Se usa como proxy el mismo criterio de
  carriles/ancho (score_carriles) ya calculado: mas carriles/ancho = mas
  margen fisico para dedicar uno.
- Contraflujo (unico criterio nuevo): depende de si se puede invadir el
  sentido contrario. Si hay separador central fisico, solo es viable en los
  huecos/cruces del separador (score bajo). Si no hay separador y la via es
  de sentido unico (oneway), el carril contrario esta vacio de trafico
  legitimo -- el escenario mas favorable. Si no hay separador y la via es de
  doble sentido, hay que invadir el carril contrario con trafico circulando
  (viable pero mas riesgoso que el caso anterior).

Resultado: 3 puntajes por segmento (score_contraflujo, score_carril_exclusivo,
score_cruce_rojo) ademas del score_con_campo compuesto que ya existia (que
sigue siendo el que alimenta el boost de prioridad en GraphHopper, ver
generar_privilegios_graphhopper.py -- esta clasificacion es para
interpretabilidad, no reemplaza esa conexion).
"""

from sqlalchemy import create_engine, text

from app.core.config import settings
from app.services.corridor_overlap import CASE_AVENIDA


def score_contraflujo(separador_central: str | None, oneway: bool) -> float | None:
    if separador_central is None:
        return None
    if separador_central == "fisico":
        return 3.0
    # ninguno o linea_pintada: se puede invadir el sentido contrario
    return 9.0 if oneway else 6.0


def main():
    engine = create_engine(settings.database_url)

    with engine.begin() as conn:
        conn.execute(text("ALTER TABLE privilegios_via ADD COLUMN IF NOT EXISTS oneway boolean"))
        conn.execute(text("ALTER TABLE privilegios_via ADD COLUMN IF NOT EXISTS score_contraflujo double precision"))
        conn.execute(text("ALTER TABLE privilegios_via ADD COLUMN IF NOT EXISTS score_carril_exclusivo double precision"))
        conn.execute(text("ALTER TABLE privilegios_via ADD COLUMN IF NOT EXISTS score_cruce_rojo double precision"))

        conn.execute(
            text(
                """
                UPDATE privilegios_via pv SET
                    oneway = oe.oneway,
                    score_carril_exclusivo = pv.score_carriles,
                    score_cruce_rojo = pv.score_semaforos
                FROM osm_edges oe
                WHERE oe.u = pv.u AND oe.v = pv.v AND oe.key = pv.key
                """
            )
        )

        segmentos = conn.execute(
            text("SELECT ctid, separador_central, oneway FROM privilegios_via")
        ).fetchall()
        for seg in segmentos:
            score = score_contraflujo(seg.separador_central, seg.oneway)
            conn.execute(
                text("UPDATE privilegios_via SET score_contraflujo = :score WHERE ctid = :ctid"),
                {"score": score, "ctid": seg.ctid},
            )

    print("Resumen por avenida (0-10, mayor = mas viable ese privilegio especifico):\n")
    with engine.connect() as conn:
        rows = conn.execute(
            text(
                f"""
                SELECT {CASE_AVENIDA} AS avenida,
                       round(avg(score_contraflujo)::numeric, 1) AS contraflujo,
                       round(avg(score_carril_exclusivo)::numeric, 1) AS carril_exclusivo,
                       round(avg(score_cruce_rojo)::numeric, 1) AS cruce_rojo,
                       round(avg(score_con_campo)::numeric, 1) AS viabilidad_general
                FROM privilegios_via
                GROUP BY avenida
                ORDER BY viabilidad_general DESC
                """
            )
        ).fetchall()
        print(f"{'Avenida':<22}{'Contraflujo':>12}{'Carril excl.':>14}{'Cruce rojo':>12}{'General':>10}")
        for r in rows:
            print(f"{r.avenida:<22}{r.contraflujo:>12}{r.carril_exclusivo:>14}{r.cruce_rojo:>12}{r.viabilidad_general:>10}")

    print("\nListo.")


if __name__ == "__main__":
    main()
