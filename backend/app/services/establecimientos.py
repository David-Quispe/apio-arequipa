"""Establecimientos de salud de Arequipa para la app (mapa y lista de hospitales).

Los datos salen de dos fuentes abiertas, unidas por
scripts/importar_establecimientos.py:

- RENIPRESS (SUSALUD, datosabiertos.gob.pe): registro oficial con categoria
  (I-1 ... III-2), institucion, estado, telefono y coordenadas.
- OpenStreetMap (Overpass API): completa las coordenadas que faltan en
  RENIPRESS y suma hospitales que estan mapeados pero no salieron del registro.

El script deja el resultado en app/data/establecimientos_arequipa.json (un
archivo chico, versionable); este modulo solo lo lee y lo filtra.
"""

import json
import unicodedata
from functools import lru_cache
from pathlib import Path

RUTA_DATOS = Path(__file__).resolve().parent.parent / "data" / "establecimientos_arequipa.json"

# Categorias con capacidad de atender una emergencia de ambulancia (segundo y
# tercer nivel, NTS N 021-MINSA). El primer nivel (I-1 a I-4) no recibe
# pacientes criticos por ambulancia.
CATEGORIAS_EMERGENCIA = {"II-1", "II-2", "II-E", "III-1", "III-2", "III-E"}

# Clasificaciones RENIPRESS que tambien cuentan como destino de emergencia
# aunque no tengan categoria (muchas clinicas privadas figuran asi).
CLASIFICACIONES_EMERGENCIA = (
    "HOSPITALES O CLINICAS",
    "INSTITUTOS DE SALUD ESPECIALIZADOS",
)


def normalizar_texto(texto: str | None) -> str:
    """Mayusculas, sin tildes ni espacios dobles (para comparar y filtrar)."""
    if not texto:
        return ""
    sin_tildes = "".join(
        c for c in unicodedata.normalize("NFKD", texto) if not unicodedata.combining(c)
    )
    return " ".join(sin_tildes.upper().split())


def tiene_emergencia(clasificacion: str | None, categoria: str | None) -> bool:
    categoria_n = normalizar_texto(categoria)
    if categoria_n in CATEGORIAS_EMERGENCIA:
        return True
    clasificacion_n = normalizar_texto(clasificacion)
    return clasificacion_n.startswith(CLASIFICACIONES_EMERGENCIA)


def tipo_desde_institucion(institucion: str | None) -> str:
    """Agrupa la institucion de RENIPRESS en los tipos que usa la app
    (cada uno con su color de marcador)."""
    inst = normalizar_texto(institucion)
    if "ESSALUD" in inst:
        return "EsSalud"
    if inst.startswith("PRIVAD"):
        return "Privada"
    if inst in {"MINSA", "GOBIERNO REGIONAL"}:
        return "MINSA"
    if "SANIDAD" in inst or "FUERZA" in inst or "POLICIA" in inst or "EJERCITO" in inst:
        return "FFAA/PNP"
    if "MUNICIPAL" in inst:
        return "Municipal"
    return "Otro"


@lru_cache(maxsize=1)
def cargar_establecimientos(ruta: str = str(RUTA_DATOS)) -> tuple[dict, ...]:
    archivo = Path(ruta)
    if not archivo.exists():
        return ()
    datos = json.loads(archivo.read_text(encoding="utf-8"))
    return tuple(datos.get("establecimientos", []))


def listar_establecimientos(solo_emergencia: bool = False, ruta: str = str(RUTA_DATOS)) -> list[dict]:
    todos = cargar_establecimientos(ruta)
    if solo_emergencia:
        return [e for e in todos if e.get("emergencia")]
    return list(todos)
