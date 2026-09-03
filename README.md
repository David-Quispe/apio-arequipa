# APIO — Enrutamiento óptimo para ambulancias en Arequipa

APIO calcula rutas para vehículos de emergencia considerando sus privilegios
legales de vía (contraflujo, carriles exclusivos, cruce de semáforo en rojo)
combinados con tráfico en tiempo real — algo que Google Maps o Waze no
modelan, porque están pensados para vehículos particulares.

Este es un proyecto de un solo desarrollador, con plazo hasta diciembre de
2026, acotado a un **corredor piloto** en Arequipa en vez de la ciudad
completa (ver [Corredor piloto](#corredor-piloto)).

## Arquitectura

| Capa | Tecnología |
|---|---|
| Frontend | React + Leaflet, como PWA |
| Backend | Python + FastAPI |
| Motor de ruteo | GraphHopper (self-hosted, custom model propio) |
| Base de datos | PostgreSQL + PostGIS |
| Datos viales | OpenStreetMap (grafo del corredor piloto) |

El backend actúa como puente entre el frontend y GraphHopper, y en el futuro
también expondrá la tabla de privilegios de emergencia por segmento vial
(en PostGIS) para que GraphHopper la use al calcular rutas.

## Corredor piloto

En vez de cubrir toda Arequipa, el proyecto se enfoca en el corredor entre
la zona de mayor crecimiento poblacional (Cono Norte) y el clúster
hospitalario del Cercado:

- **Origen:** Cono Norte (Cerro Colorado -incluyendo Peruarbo, Río Seco y
  Zamácola-, Cayma, Yanahuara)
- **Destinos:** Hospital Honorio Delgado Espinoza (MINSA), Hospital
  Goyeneche (MINSA), Hospital Nacional Carlos Alberto Seguín Escobedo
  (EsSalud), Clínica Arequipa (privada)
- **Vías principales:** Av. Ejército, Puente Grau, Av. Bolognesi, Av.
  Dolores, Av. Daniel Alcides Carrión, Av. Goyeneche/Peral/El Filtro

El área de cobertura real (grafo cargado en GraphHopper/PostGIS) es el
rectángulo `-71.63,-16.42` a `-71.52,-16.33` — un origen o destino fuera de
ese rectángulo no tiene ruta calculable (ver `CORRIDOR_BBOX` en
`backend/scripts/load_osm_graph.py`).

Más contexto y el cronograma completo en
[Proyecto_APIO_Resumen_Planificacion.docx](Proyecto_APIO_Resumen_Planificacion.docx).

## Estructura del repositorio

```
backend/    API FastAPI (proxy hacia GraphHopper, futura tabla de privilegios)
frontend/   Mapa React + Leaflet del corredor piloto
infra/      docker-compose.yml (PostgreSQL + PostGIS)
routing/    Configuración de GraphHopper (custom model, config.yml)
```

## Cómo levantar el entorno local

**Atajo:** una vez que backend y GraphHopper ya están configurados (pasos 2 y 3
hechos al menos una vez), `./start.ps1` levanta Docker, GraphHopper y el
backend de una sola vez (detecta lo que ya esté corriendo). `./stop.ps1` los
detiene (agregá `-Todo` para bajar también la base de datos). El frontend se
deja aparte (`cd frontend; npm run dev`) para ver su output en la terminal.

**1. Base de datos (PostgreSQL + PostGIS)**
```bash
cd infra
docker compose up -d
```

**2. Backend**
```bash
cd backend
python -m venv .venv
./.venv/Scripts/pip install -r requirements.txt
cp .env.example .env  # y completar TOMTOM_API_KEY (gratis en developer.tomtom.com)
./.venv/Scripts/python -m uvicorn app.main:app --reload --port 8000
```

**3. GraphHopper** (requiere Java 17+; el jar y el extracto OSM no están en
el repo por su tamaño, ver `routing/config.yml` para dónde deben ir)
```bash
cd routing
java -Xmx3g -jar graphhopper-web-11.0.jar server config.yml
```

**4. Frontend**
```bash
cd frontend
npm install
npm run dev
```

## Demo sin servidor propio

Para mostrar APIO sin depender de que la laptop de David esté prendida con
todo el stack corriendo, hay un **modo demo**: mismo frontend/mapa, pero sin
GraphHopper/PostGIS reales detrás.

- Las rutas son un set fijo precalculado (4 orígenes del Cono Norte × 4
  hospitales, `frontend/src/demo/rutas.json`, generado corriendo
  `backend/scripts/generar_demo_rutas.py` contra el backend real) — solo se
  puede elegir origen/destino de esas listas, no cualquier punto del mapa.
- El **tráfico y el ETA sí son reales**: dos funciones serverless
  (`frontend/api/route.js`, `frontend/api/traffic.js`) consultan TomTom en
  el momento y aplican la misma fórmula que `backend/app/api/routes/route.py`
  (duplicada a propósito en JS, ver comentario en `frontend/api/_trafico.js`).
- Se activa con la variable de entorno `VITE_DEMO_MODE=true` en el build
  (`frontend/src/main.jsx` elige entre `App.jsx` y `DemoApp.jsx`); en local
  nunca se define, así que `npm run dev` siempre usa la app real.

**Probarlo en local antes de desplegar:**
```bash
cd frontend
TOMTOM_API_KEY=... npm run dev:demo-api   # sirve /api/route y /api/traffic en :8787
npm run dev:demo                          # vite en modo demo, con proxy /api -> :8787
```

**Desplegar en Vercel (gratis):**
1. Crear una cuenta en [vercel.com](https://vercel.com) (con GitHub) e importar
   el repo `apio-arequipa`.
2. En la configuración del proyecto, "Root Directory" → `frontend`.
3. Agregar dos variables de entorno: `VITE_DEMO_MODE=true` y
   `TOMTOM_API_KEY=...` (la de `developer.tomtom.com`, la misma del backend).
4. Deploy. Vercel detecta Vite automáticamente y sirve `frontend/api/*.js`
   como funciones serverless sin configuración adicional.

Si cambia la rúbrica de privilegios o el corredor, hay que volver a correr
`generar_demo_rutas.py` y redeployar para que la demo quede al día — no se
actualiza sola (a diferencia del tráfico, que sí es en vivo).

## Estado actual

- [x] Backend, frontend, base de datos y motor de ruteo funcionando end-to-end
- [x] Grafo vial del corredor piloto cargado en PostGIS y en GraphHopper
- [x] Mapa interactivo: selección de origen y cálculo de ruta real
- [x] Buscador de dirección, PWA instalable, iconos por tipo de hospital
- [x] Rúbrica de privilegios: 5 criterios (jerarquía vial, carriles/ancho,
      semáforos por km, separador central, congestión observada) calculados
      sobre los ~156 segmentos reales del corredor (tabla `privilegios_via` en
      PostGIS). La congestión es de una sola observación puntual, no una
      medición repetida — punto de partida, no definitiva.
- [x] `custom_model` de GraphHopper ajustado para preferir vías de mayor
      jerarquía y más carriles (2 de los 5 criterios, aplicados a toda la red)
- [x] Tráfico en tiempo real (TomTom Traffic Flow API): endpoint `/api/traffic`
      con congestión actual por avenida (cacheada 60s), panel en el mapa, y
      ETA de `/api/route` ajustado según las avenidas que la ruta realmente
      cruza (ST_Intersects sobre `privilegios_via`)
- [x] Privilegios de vía conectados al ruteo real: las 8 avenidas del
      corredor son "áreas" nativas del `custom_model` de GraphHopper
      (`backend/scripts/generar_privilegios_graphhopper.py`), con un plus de
      prioridad proporcional a su `score_con_campo` — la ruta elegida ahora
      sí prefiere las vías donde es más viable ejercer el privilegio legal,
      no solo las de mayor jerarquía genérica. `/api/route` devuelve
      `privilegios_cruzados` (avenida + score) y se muestra en el mapa.
- [x] Clasificación por tipo de privilegio específico: además del puntaje
      compuesto de viabilidad, cada avenida tiene un puntaje propio para
      contraflujo, carril exclusivo y cruce de semáforo en rojo
      (`backend/scripts/clasificar_privilegios_especificos.py`), y `/api/route`
      indica cuál es el más viable en cada una (ej. "Av. Ejército — carril
      exclusivo 6.6/10" en vez de un número genérico de "viabilidad")
- [x] Manejo de origen/destino fuera del área piloto: `/api/route` distingue
      el error de GraphHopper (`PointNotFoundException`/`PointOutOfBoundsException`)
      y devuelve un mensaje claro en vez de un error genérico
- [x] El ETA de la ruta activa se refresca solo cada 25s (mismo intervalo que
      el panel de tráfico), sin esperar a que el usuario pida una ruta nueva.
      Sigue sin recalcularse el *camino* elegido (eso es una decisión de
      diseño ya documentada — el tráfico ajusta el ETA, no la ruta)
- [x] Modo demo desplegable gratis (Vercel, sin backend/GraphHopper propios
      corriendo) con rutas fijas precalculadas pero tráfico/ETA en vivo — ver
      [Demo sin servidor propio](#demo-sin-servidor-propio)
