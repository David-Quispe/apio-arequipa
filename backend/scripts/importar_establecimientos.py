"""Genera app/data/establecimientos_arequipa.json uniendo RENIPRESS + OpenStreetMap.

Uso (desde backend/, con el venv activo):

    python scripts/importar_establecimientos.py
    python scripts/importar_establecimientos.py --sin-osm            # solo RENIPRESS
    python scripts/importar_establecimientos.py --incluir-consultorios

Que hace:
1. Lee el CSV de RENIPRESS (SUSALUD, datosabiertos.gob.pe) que esta en
   backend/fuentes/ y se queda con los establecimientos ACTIVOS de la
   provincia de Arequipa. Descarta los servicios medicos de apoyo
   (laboratorios, opticas, etc.) y, por defecto, los consultorios privados,
   que no son destinos de una ambulancia.
2. Marca cuales tienen emergencia (categoria II/III o clasificacion
   "hospitales o clinicas"), ver app/services/establecimientos.py.
3. Consulta la Overpass API de OpenStreetMap y, para los que no traen
   coordenadas en RENIPRESS, toma las del punto de OSM con el nombre mas
   parecido. Tambien suma los hospitales de OSM que no estan en el registro.
4. Escribe el JSON final (chico, se puede subir a git) e imprime un resumen.

Los CSV originales pesan varios MB: quedan fuera de git (ver .gitignore).
"""

import argparse
import csv
import json
import math
import sys
from datetime import datetime, timezone
from difflib import SequenceMatcher
from pathlib import Path

import httpx

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from app.services.establecimientos import (  # noqa: E402
    RUTA_DATOS,
    normalizar_texto,
    tiene_emergencia,
    tipo_desde_institucion,
)

RAIZ_BACKEND = Path(__file__).resolve().parent.parent
CSV_POR_DEFECTO = RAIZ_BACKEND / "fuentes" / "RENIPRESS_2026_v8.csv"
OVERPASS_URL = "https://overpass-api.de/api/interpreter"

# Caja de la provincia de Arequipa (sur, oeste, norte, este): cubre la ciudad
# y los distritos donde GraphHopper puede calcular rutas.
BBOX_PROVINCIA = (-16.75, -71.85, -16.15, -71.25)
# Caja amplia del departamento, solo para descartar coordenadas absurdas.
BBOX_DEPARTAMENTO = (-17.5, -75.2, -14.5, -70.7)

# Palabras genericas que se quitan antes de comparar nombres entre fuentes
PALABRAS_GENERICAS = {
    "HOSPITAL", "CLINICA", "CENTRO", "DE", "SALUD", "PUESTO", "POSTA", "MEDICO",
    "C.S.", "P.S.", "CS", "PS", "EL", "LA", "LOS", "LAS", "DEL", "Y", "SAC", "S.A.C.", "EIRL",
}


def nombre_clave(nombre: str) -> str:
    palabras = [p for p in normalizar_texto(nombre).replace("-", " ").split() if p not in PALABRAS_GENERICAS]
    return " ".join(palabras)


def a_float(valor: str | None) -> float | None:
    try:
        numero = float(str(valor).replace(",", ".").strip())
    except (TypeError, ValueError):
        return None
    return numero if numero != 0 else None


def dentro(lat: float, lon: float, caja: tuple[float, float, float, float]) -> bool:
    sur, oeste, norte, este = caja
    return sur <= lat <= norte and oeste <= lon <= este


def coordenadas_renipress(fila: dict) -> tuple[float, float] | None:
    """En RENIPRESS 2026 NORTE es la latitud y ESTE la longitud, pero algunas
    filas vienen invertidas: se corrige si el par cae al reves."""
    norte, este = a_float(fila.get("NORTE")), a_float(fila.get("ESTE"))
    if norte is None or este is None:
        return None
    if dentro(norte, este, BBOX_DEPARTAMENTO):
        return norte, este
    if dentro(este, norte, BBOX_DEPARTAMENTO):
        return este, norte
    return None


def distancia_m(a: tuple[float, float], b: tuple[float, float]) -> float:
    lat1, lon1, lat2, lon2 = map(math.radians, (*a, *b))
    h = math.sin((lat2 - lat1) / 2) ** 2 + math.cos(lat1) * math.cos(lat2) * math.sin((lon2 - lon1) / 2) ** 2
    return 2 * 6_371_000 * math.asin(math.sqrt(h))


def leer_renipress(ruta_csv: Path, provincia: str, incluir_consultorios: bool) -> tuple[list[dict], dict]:
    conteo = {"filas_provincia": 0, "inactivos": 0, "apoyo": 0, "consultorios": 0, "duplicados": 0}
    resultado = []
    codigos_vistos: set[str] = set()
    with ruta_csv.open(encoding="utf-8-sig", newline="") as archivo:
        for fila in csv.DictReader(archivo, delimiter=";"):
            if normalizar_texto(fila.get("DEPARTAMENTO")) != "AREQUIPA":
                continue
            if normalizar_texto(fila.get("PROVINCIA")) != provincia:
                continue
            conteo["filas_provincia"] += 1
            if normalizar_texto(fila.get("ESTADO")) not in {"ACTIVO", "ACTIVADO"}:
                conteo["inactivos"] += 1
                continue
            tipo_est = normalizar_texto(fila.get("TIPO_ESTABLECIMIENTO"))
            clasificacion = normalizar_texto(fila.get("CLASIFICACION"))
            if "APOYO" in tipo_est:
                conteo["apoyo"] += 1
                continue
            if clasificacion.startswith("CONSULTORIOS") and not incluir_consultorios:
                conteo["consultorios"] += 1
                continue

            codigo = (fila.get("COD_IPRESS") or "").strip()
            if codigo and codigo in codigos_vistos:
                conteo["duplicados"] += 1
                continue
            if codigo:
                codigos_vistos.add(codigo)

            coords = coordenadas_renipress(fila)
            categoria = (fila.get("CATEGORIA") or "").strip() or None
            resultado.append({
                "codigo": codigo or None,
                "nombre": " ".join((fila.get("NOMBRE") or "").split()),
                "tipo": tipo_desde_institucion(fila.get("INSTITUCION")),
                "institucion": (fila.get("INSTITUCION") or "").strip() or None,
                "clasificacion": (fila.get("CLASIFICACION") or "").strip() or None,
                "categoria": categoria if categoria and categoria.upper() != "SIN CATEGORIA" else None,
                "emergencia": tiene_emergencia(fila.get("CLASIFICACION"), categoria),
                "distrito": (fila.get("DISTRITO") or "").strip().title() or None,
                "direccion": (fila.get("DIRECCION") or "").strip() or None,
                "telefono": (fila.get("TELEFONO") or "").strip() or None,
                "horario": (fila.get("HORARIO") or "").strip() or None,
                "lat": coords[0] if coords else None,
                "lon": coords[1] if coords else None,
                "fuente_coordenadas": "renipress" if coords else None,
            })
    return resultado, conteo


def consultar_osm() -> list[dict]:
    sur, oeste, norte, este = BBOX_PROVINCIA
    caja = f"{sur},{oeste},{norte},{este}"
    consulta = f"""
    [out:json][timeout:90];
    (
      nwr["amenity"~"^(hospital|clinic|doctors)$"]({caja});
      nwr["healthcare"~"^(hospital|clinic|centre)$"]({caja});
    );
    out center tags;
    """
    respuesta = httpx.post(
        OVERPASS_URL,
        data={"data": consulta},
        headers={"User-Agent": "APIO-ambulancias-Arequipa/1.0 (proyecto academico TECSUP)"},
        timeout=120,
    )
    respuesta.raise_for_status()
    puntos = []
    for elemento in respuesta.json().get("elements", []):
        etiquetas = elemento.get("tags", {})
        nombre = etiquetas.get("name")
        lat = elemento.get("lat") or elemento.get("center", {}).get("lat")
        lon = elemento.get("lon") or elemento.get("center", {}).get("lon")
        if not nombre or lat is None or lon is None:
            continue
        puntos.append({
            "osm_id": f"{elemento['type']}/{elemento['id']}",
            "nombre": nombre,
            "clave": nombre_clave(nombre),
            "es_hospital": etiquetas.get("amenity") == "hospital" or etiquetas.get("healthcare") == "hospital",
            "operador_privado": etiquetas.get("operator:type") == "private",
            "categoria": etiquetas.get("ref:category"),
            "lat": float(lat),
            "lon": float(lon),
        })
    return puntos


def mejor_coincidencia(nombre: str, puntos_osm: list[dict], usados: set[str]) -> dict | None:
    clave = nombre_clave(nombre)
    if len(clave) < 4:
        return None
    mejor, mejor_ratio = None, 0.0
    for punto in puntos_osm:
        if punto["osm_id"] in usados or not punto["clave"]:
            continue
        ratio = SequenceMatcher(None, clave, punto["clave"]).ratio()
        if ratio > mejor_ratio:
            mejor, mejor_ratio = punto, ratio
    return mejor if mejor_ratio >= 0.85 else None


def unir_con_osm(establecimientos: list[dict], puntos_osm: list[dict]) -> dict:
    conteo = {"coordenadas_desde_osm": 0, "hospitales_solo_osm": 0}
    usados: set[str] = set()

    # 1. completar coordenadas faltantes con el punto de OSM de nombre mas parecido
    for est in establecimientos:
        if est["lat"] is not None:
            continue
        punto = mejor_coincidencia(est["nombre"], puntos_osm, usados)
        if punto:
            est["lat"], est["lon"] = punto["lat"], punto["lon"]
            est["fuente_coordenadas"] = "osm"
            usados.add(punto["osm_id"])
            conteo["coordenadas_desde_osm"] += 1

    # 2. sumar hospitales que estan en OSM pero no en el registro (sin duplicar:
    #    se descartan si ya hay un establecimiento a menos de 150 m)
    con_coordenadas = [(e["lat"], e["lon"]) for e in establecimientos if e["lat"] is not None]
    for punto in puntos_osm:
        if not punto["es_hospital"] or punto["osm_id"] in usados:
            continue
        posicion = (punto["lat"], punto["lon"])
        if any(distancia_m(posicion, otro) < 150 for otro in con_coordenadas):
            continue
        establecimientos.append({
            "codigo": None,
            "nombre": punto["nombre"],
            "tipo": "Privada" if punto["operador_privado"] else "Otro",
            "institucion": None,
            "clasificacion": "HOSPITAL (OpenStreetMap)",
            "categoria": punto["categoria"],
            "emergencia": True,
            "distrito": None,
            "direccion": None,
            "telefono": None,
            "horario": None,
            "lat": punto["lat"],
            "lon": punto["lon"],
            "fuente_coordenadas": "osm",
        })
        con_coordenadas.append(posicion)
        conteo["hospitales_solo_osm"] += 1
    return conteo


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--csv", type=Path, default=CSV_POR_DEFECTO, help="CSV de RENIPRESS (separado por ;)")
    parser.add_argument("--salida", type=Path, default=RUTA_DATOS)
    parser.add_argument("--provincia", default="AREQUIPA")
    parser.add_argument("--sin-osm", action="store_true", help="no consultar OpenStreetMap")
    parser.add_argument("--incluir-consultorios", action="store_true")
    args = parser.parse_args()

    if not args.csv.exists():
        sys.exit(f"No encuentro el CSV de RENIPRESS en {args.csv}")

    establecimientos, conteo = leer_renipress(args.csv, normalizar_texto(args.provincia), args.incluir_consultorios)
    print(f"RENIPRESS: {conteo['filas_provincia']} filas en la provincia, {len(establecimientos)} utiles")
    print(
        f"  descartados -> inactivos: {conteo['inactivos']}, servicios de apoyo: {conteo['apoyo']}, "
        f"consultorios: {conteo['consultorios']}, duplicados: {conteo['duplicados']}"
    )

    conteo_osm = {}
    if not args.sin_osm:
        try:
            puntos_osm = consultar_osm()
            print(f"OpenStreetMap: {len(puntos_osm)} puntos de salud con nombre")
            conteo_osm = unir_con_osm(establecimientos, puntos_osm)
            print(
                f"  coordenadas completadas con OSM: {conteo_osm['coordenadas_desde_osm']}, "
                f"hospitales agregados solo desde OSM: {conteo_osm['hospitales_solo_osm']}"
            )
        except httpx.HTTPError as error:
            print(f"  (aviso) no se pudo consultar Overpass: {error}. Sigo solo con RENIPRESS.")

    sin_coordenadas = [e for e in establecimientos if e["lat"] is None]
    finales = [e for e in establecimientos if e["lat"] is not None and dentro(e["lat"], e["lon"], BBOX_PROVINCIA)]
    finales.sort(key=lambda e: (not e["emergencia"], e["nombre"]))

    salida = {
        "generado": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "fuentes": [
            "RENIPRESS - SUSALUD (datosabiertos.gob.pe)",
            "OpenStreetMap contributors (Overpass API), ODbL",
        ],
        "provincia": args.provincia.title(),
        "total": len(finales),
        "con_emergencia": sum(1 for e in finales if e["emergencia"]),
        "establecimientos": finales,
    }
    args.salida.parent.mkdir(parents=True, exist_ok=True)
    args.salida.write_text(json.dumps(salida, ensure_ascii=False, indent=1), encoding="utf-8")

    print(f"\nListo: {salida['total']} establecimientos ({salida['con_emergencia']} con emergencia) -> {args.salida}")
    print(f"Sin coordenadas en ninguna fuente (quedan fuera): {len(sin_coordenadas)}")
    for est in sin_coordenadas[:10]:
        marca = " [EMERGENCIA]" if est["emergencia"] else ""
        print(f"  - {est['nombre']} ({est['categoria'] or 's/c'}){marca}")


if __name__ == "__main__":
    main()
