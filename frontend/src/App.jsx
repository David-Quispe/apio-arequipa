import { useState } from 'react'
import { MapContainer, TileLayer, Marker, Popup, Polyline, useMapEvents } from 'react-leaflet'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import './App.css'

const API_BASE = 'http://localhost:8000/api'

// Corredor piloto confirmado: clúster hospitalario del Cercado de Arequipa
// Coordenadas verificadas contra el polígono del edificio en OpenStreetMap
// (Overpass API, tags amenity=hospital/clinic), no geocodificación aproximada.
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

const iconoAmbulancia = new L.DivIcon({
  className: '',
  html: '<div style="width:18px;height:18px;border-radius:50%;background:#e11d48;border:3px solid white;box-shadow:0 0 4px rgba(0,0,0,0.5);"></div>',
  iconSize: [18, 18],
  iconAnchor: [9, 9],
})

function SelectorDeOrigen({ onSelect }) {
  useMapEvents({
    click(e) {
      onSelect([e.latlng.lat, e.latlng.lng])
    },
  })
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

function App() {
  const [origen, setOrigen] = useState(ORIGEN_INICIAL)
  const [ruta, setRuta] = useState(null)
  const [destinoActivo, setDestinoActivo] = useState(null)
  const [cargando, setCargando] = useState(false)
  const [error, setError] = useState(null)

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
    setRuta(null)
    setDestinoActivo(null)
  }

  return (
    <div id="map-container" style={{ height: '100vh', width: '100vw', position: 'relative' }}>
      <div className="panel">
        <strong>Proyecto APIO — demo de ruteo</strong>
        <p>Clic en el mapa para mover el origen (ambulancia). Clic en un hospital para calcular la ruta.</p>
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

        <Marker position={origen} icon={iconoAmbulancia}>
          <Popup>Origen (ambulancia)</Popup>
        </Marker>

        {DESTINOS.map((destino) => (
          <Marker
            key={destino.nombre}
            position={destino.posicion}
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
