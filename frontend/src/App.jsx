import { useEffect, useState } from 'react'
import { MapContainer, TileLayer, Marker, Popup, Polyline, useMap, useMapEvents } from 'react-leaflet'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import './App.css'

const API_BASE = 'http://localhost:8000/api'

// Bbox del corredor piloto (west, south, east, north) - mismo usado para
// descargar el grafo vial en GraphHopper y PostGIS.
const CORRIDOR_VIEWBOX = '-71.585,-16.360,-71.520,-16.415'

const TIPO_COLORES = {
  MINSA: '#2563eb',
  EsSalud: '#059669',
  Privada: '#7c3aed',
}

// Coordenadas verificadas contra el poligono del edificio en OpenStreetMap
// (Overpass API, tags amenity=hospital/clinic), no geocodificacion aproximada.
const DESTINOS = [
  {
    nombre: 'Hospital Regional Honorio Delgado Espinoza',
    tipo: 'MINSA',
    posicion: [-16.4149129, -71.5315582],
  },
  {
    nombre: 'Hospital Goyeneche',
    tipo: 'MINSA',
    posicion: [-16.4024925, -71.5279176],
  },
  {
    nombre: 'Hospital Nacional Carlos Alberto Seguín Escobedo',
    tipo: 'EsSalud',
    posicion: [-16.3952624, -71.5307408],
  },
  {
    nombre: 'Clínica Arequipa',
    tipo: 'Privada',
    posicion: [-16.391834, -71.5400938],
  },
]

const CENTRO_AREQUIPA = [-16.3989, -71.5369]
const ORIGEN_INICIAL = [-16.3833, -71.55] // Cerro Colorado, zona de origen del corredor

function crearIconoCircular({ emoji, color, size = 30 }) {
  return new L.DivIcon({
    className: '',
    html: `<div style="width:${size}px;height:${size}px;border-radius:50%;background:${color};border:2px solid white;box-shadow:0 1px 4px rgba(0,0,0,0.45);display:flex;align-items:center;justify-content:center;font-size:${size * 0.55}px;line-height:1;">${emoji}</div>`,
    iconSize: [size, size],
    iconAnchor: [size / 2, size / 2],
    popupAnchor: [0, -size / 2],
  })
}

const iconoAmbulancia = crearIconoCircular({ emoji: '🚑', color: '#111827', size: 34 })
const iconosHospital = Object.fromEntries(
  Object.entries(TIPO_COLORES).map(([tipo, color]) => [tipo, crearIconoCircular({ emoji: '🏥', color, size: 30 })])
)

function SelectorDeOrigen({ onSelect }) {
  useMapEvents({
    click(e) {
      onSelect([e.latlng.lat, e.latlng.lng])
    },
  })
  return null
}

function ControladorDeMapa({ centrarEn }) {
  const map = useMap()
  useEffect(() => {
    if (centrarEn) {
      map.flyTo(centrarEn, Math.max(map.getZoom(), 15), { duration: 0.8 })
    }
  }, [centrarEn, map])
  return null
}

async function calcularRuta(origen, destino) {
  const params = new URLSearchParams({
    origin_lat: origen[0],
    origin_lon: origen[1],
    dest_lat: destino[0],
    dest_lon: destino[1],
  })
  const res = await fetch(`${API_BASE}/route?${params}`)
  if (!res.ok) throw new Error('No se pudo calcular la ruta')
  return res.json()
}

async function buscarDireccion(query) {
  const params = new URLSearchParams({
    format: 'json',
    q: query,
    viewbox: CORRIDOR_VIEWBOX,
    bounded: '1',
    limit: '1',
    countrycodes: 'pe',
  })
  const res = await fetch(`https://nominatim.openstreetmap.org/search?${params}`)
  if (!res.ok) throw new Error('No se pudo buscar la dirección')
  const resultados = await res.json()
  if (resultados.length === 0) throw new Error('Dirección no encontrada en el corredor piloto')
  return [parseFloat(resultados[0].lat), parseFloat(resultados[0].lon)]
}

function App() {
  const [origen, setOrigen] = useState(ORIGEN_INICIAL)
  const [centrarEn, setCentrarEn] = useState(null)
  const [ruta, setRuta] = useState(null)
  const [destinoActivo, setDestinoActivo] = useState(null)
  const [cargando, setCargando] = useState(false)
  const [error, setError] = useState(null)
  const [busqueda, setBusqueda] = useState('')
  const [buscando, setBuscando] = useState(false)

  const handleSeleccionarDestino = async (destino) => {
    setCargando(true)
    setError(null)
    setDestinoActivo(destino.nombre)
    try {
      const resultado = await calcularRuta(origen, destino.posicion)
      // GeoJSON viene como [lon, lat]; Leaflet necesita [lat, lon]
      const puntos = resultado.geometry.coordinates.map(([lon, lat]) => [lat, lon])
      setRuta({ puntos, distancia_m: resultado.distance_m, tiempo_s: resultado.time_s })
    } catch (e) {
      setError(e.message)
      setRuta(null)
    } finally {
      setCargando(false)
    }
  }

  const handleSeleccionarOrigen = (nuevoOrigen) => {
    setOrigen(nuevoOrigen)
    setCentrarEn(null)
    setRuta(null)
    setDestinoActivo(null)
  }

  const handleBuscar = async (e) => {
    e.preventDefault()
    if (!busqueda.trim()) return
    setBuscando(true)
    setError(null)
    try {
      const encontrado = await buscarDireccion(busqueda)
      setOrigen(encontrado)
      setCentrarEn(encontrado)
      setRuta(null)
      setDestinoActivo(null)
    } catch (e) {
      setError(e.message)
    } finally {
      setBuscando(false)
    }
  }

  return (
    <div id="map-container">
      <div className="panel">
        <strong>APIO — demo de ruteo</strong>
        <form className="panel-busqueda" onSubmit={handleBuscar}>
          <input
            type="text"
            placeholder="Buscar dirección de origen..."
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
          />
          <button type="submit" disabled={buscando}>
            {buscando ? '...' : 'Buscar'}
          </button>
        </form>
        <p className="panel-ayuda">O hacé clic en el mapa para mover el origen. Clic en un hospital para calcular la ruta.</p>
        {cargando && <p>Calculando ruta...</p>}
        {error && <p className="panel-error">{error}</p>}
        {ruta && !cargando && (
          <p>
            <strong>{destinoActivo}</strong>
            <br />
            {(ruta.distancia_m / 1000).toFixed(2)} km · {(ruta.tiempo_s / 60).toFixed(1)} min
          </p>
        )}
      </div>

      <MapContainer center={CENTRO_AREQUIPA} zoom={14} style={{ height: '100%', width: '100%' }}>
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />

        <SelectorDeOrigen onSelect={handleSeleccionarOrigen} />
        <ControladorDeMapa centrarEn={centrarEn} />

        <Marker position={origen} icon={iconoAmbulancia}>
          <Popup>Origen (ambulancia)</Popup>
        </Marker>

        {DESTINOS.map((destino) => (
          <Marker
            key={destino.nombre}
            position={destino.posicion}
            icon={iconosHospital[destino.tipo]}
            eventHandlers={{ click: () => handleSeleccionarDestino(destino) }}
          >
            <Popup>
              <strong>{destino.nombre}</strong>
              <br />
              {destino.tipo}
            </Popup>
          </Marker>
        ))}

        {ruta && <Polyline positions={ruta.puntos} pathOptions={{ color: '#e11d48', weight: 5 }} />}
      </MapContainer>
    </div>
  )
}

export default App
