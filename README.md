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

- **Origen:** Cayma, Yanahuara, Cerro Colorado
- **Destinos:** Hospital Honorio Delgado Espinoza (MINSA), Hospital
  Goyeneche (MINSA), Hospital Nacional Carlos Alberto Seguín Escobedo
  (EsSalud), Clínica Arequipa (privada)
- **Vías principales:** Av. Ejército, Puente Grau, Av. Bolognesi, Av.
  Dolores, Av. Daniel Alcides Carrión, Av. Goyeneche/Peral/El Filtro

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
./.venv/Scripts/python -m uvicorn app.main:app --reload --port 8000
```

**3. GraphHopper** (requiere Java 17+; el jar y el extracto OSM no están en
el repo por su tamaño, ver `routing/config.yml` para dónde deben ir)
```bash
cd routing
java -Xmx2g -jar graphhopper-web-11.0.jar server config.yml
```

**4. Frontend**
```bash
cd frontend
npm install
npm run dev
```

## Estado actual

- [x] Backend, frontend, base de datos y motor de ruteo funcionando end-to-end
- [x] Grafo vial del corredor piloto cargado en PostGIS y en GraphHopper
- [x] Mapa interactivo: selección de origen y cálculo de ruta real
- [ ] Rúbrica de clasificación de privilegios por segmento vial (pendiente)
- [ ] Tabla de privilegios en PostGIS integrada al modelo de costos de GraphHopper
- [ ] Tráfico en tiempo real
