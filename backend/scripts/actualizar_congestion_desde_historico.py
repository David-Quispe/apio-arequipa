"""
Reemplaza la observacion puntual de congestion_tipica (Fase 1, una sola
inspeccion visual) por el promedio de las muestras reales de trafico
acumuladas en data/trafico_historico.jsonl -- recolectadas automaticamente
por .github/workflows/muestreo-trafico.yml contra la demo en Vercel, sin
depender de que el backend local este corriendo.

Correr esto DESPUES de acumular varias muestras en distintos horarios/dias
(cuantas mas y mas variadas, mejor) -- con pocas muestras no aporta mas
que la observacion puntual que reemplaza. El workflow corre cada 3 horas;
unos pocos dias ya dan una cobertura horaria razonable.
"""

import json
import statistics
from collections import defaultdict
from pathlib import Path

from sqlalchemy import create_engine, text

from app.core.config import settings
from app.services.corridor_overlap import CASE_AVENIDA
from app.services.traffic import nivel
from scripts.actualizar_rubrica_campo import imprimir_resumen, recalcular_score_con_campo

MINIMO_MUESTRAS_RECOMENDADO = 10

HISTORICO = Path(__file__).resolve().parents[2] / "data" / "trafico_historico.jsonl"


def leer_ratios_por_avenida() -> dict[str, list[float]]:
    ratios = defaultdict(list)
    with open(HISTORICO, encoding="utf-8") as f:
        for linea in f:
            linea = linea.strip()
            if not linea:
                continue
            registro = json.loads(linea)
            for avenida in registro["avenidas"]:
                if avenida.get("nivel") != "sin_datos":
                    ratios[avenida["avenida"]].append(avenida["ratio"])
    return ratios


def main():
    if not HISTORICO.exists():
        print(f"No existe {HISTORICO} todavia -- esperar a que el workflow de GitHub Actions acumule muestras.")
        return

    ratios_por_avenida = leer_ratios_por_avenida()
    n_muestras = max((len(r) for r in ratios_por_avenida.values()), default=0)
    print(f"{n_muestras} muestras encontradas en el historico.\n")
    if n_muestras < MINIMO_MUESTRAS_RECOMENDADO:
        print(
            f"Advertencia: menos de {MINIMO_MUESTRAS_RECOMENDADO} muestras todavia -- "
            "el resultado puede no ser mas representativo que la observacion puntual anterior.\n"
        )

    engine = create_engine(settings.database_url)
    with engine.begin() as conn:
        for avenida, ratios in ratios_por_avenida.items():
            promedio = statistics.mean(ratios)
            nivel_congestion = nivel(promedio)
            print(f"  {avenida}: ratio promedio {promedio:.2f} ({len(ratios)} muestras) -> congestion_tipica = {nivel_congestion}")
            conn.execute(
                text(f"UPDATE privilegios_via SET congestion_tipica = :nivel WHERE ({CASE_AVENIDA}) = :avenida"),
                {"nivel": nivel_congestion, "avenida": avenida},
            )

        recalcular_score_con_campo(conn)

    imprimir_resumen(engine)
    print("\nListo.")


if __name__ == "__main__":
    main()
