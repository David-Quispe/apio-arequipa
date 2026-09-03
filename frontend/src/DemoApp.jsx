import { useEffect, useRef, useState } from 'react'
import { MapContainer, TileLayer, Marker, Popup, Polyline } from 'react-leaflet'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import './App.css'
import rutasData from './demo/rutas.json'

// Modo demo: se usa cuando VITE_DEMO_MODE=true en el build (deploy sin
// servidor propio en Vercel, ver README "Demo sin servidor propio"). No hay
// GraphHopper/PostGIS reales corriendo detras -- las rutas son un set fijo
// precalculado (api/route.js les suma tráfico en vivo de TomTom), asi que
// el origen/destino se eligen de listas fijas en vez de clic libre en el
// mapa o buscador de direcciones.

const TIPO_COLORES = {
  MINSA: '#2563eb',
  EsSalud: '#059669',
  Privada: '#7c3aed',
}

const CENTRO_AREQUIPA = [-16.3989, -71.5369]
const TRAFICO_INTERVALO_MS = 25000
const NIVEL_COLORES = {
  baja: '#059669',
  media: '#d97706',
  alta: '#dc2626',
  sin_datos: '#9ca3af',
}

const ORIGENES = rutasData.origenes
const DESTINOS = rutasData.destinos

function crearIconoCircular({ emoji, color, size = 30, atenuado = false }) {
  return new L.DivIcon({
    className: '',
    html: `<div style="width:${size}px;height:${size}px;border-radius:50%;background:${color};border:2px solid white;box-shadow:0 1px 4px rgba(0,0,0,0.45);display:flex;align-items:center;justify-content:center;font-size:${size * 0.55}px;line-height:1;opacity:${atenuado ? 0.55 : 1};">${emoji}</div>`,
    iconSize: [size, size],
    iconAnchor: [size / 2, size / 2],
    popupAnchor: [0, -size / 2],
  })
}

const iconosHospital = Object.fromEntries(
  Object.entries(TIPO_COLORES).map(([tipo, color]) => [tipo, crearIconoCircular({ emoji: '🏥', color, size: 30 })])
)
const iconoOrigenActivo = crearIconoCircular({ emoji: '🚑', color: '#111827', size: 34 })
const iconoOrigenInactivo = crearIconoCircular({ emoji: '🚑', color: '#111827', size: 24, atenuado: true })

async function calcularRutaDemo(origenId, destinoId) {
  const params = new URLSearchParams({ origen: origenId, destino: destinoId })
  const res = await fetch(`/api/route?${params}`)
  if (!res.ok) {
    const body = await res.json().catch(() => null)
    throw new Error(body?.detail || 'No se pudo calcular la ruta')
  }
  return res.json()
}

async function obtenerTraficoDemo() {
  const res = await fetch('/api/traffic')
  if (!res.ok) throw new Error('No se pudo obtener el tráfico')
  const data = await res.json()
  return data.avenidas
}

function DemoApp() {
  const [origenId, setOrigenId] = useState(ORIGENES[0].id)
  const [destinoId, setDestinoId] = useState(null)
  const [ruta, setRuta] = useState(null)
  const [cargando, setCargando] = useState(false)
  const [error, setError] = useState(null)
  const [trafico, setTrafico] = useState([])

  const origenIdRef = useRef(origenId)
  const destinoIdRef = useRef(null)

  useEffect(() => {
    origenIdRef.current = origenId
  }, [origenId])

  useEffect(() => {
    let activo = true
    const actualizar = async () => {
      try {
        const data = await obtenerTraficoDemo()
        if (!activo) return
        setTrafico(data)

        if (destinoIdRef.current) {
          const resultado = await calcularRutaDemo(origenIdRef.current, destinoIdRef.current)
          if (!activo || !destinoIdRef.current) return
          setRuta((actual) =>
            actual && {
              ...actual,
              tiempo_s_con_trafico: resultado.time_s_con_trafico,
              privilegios_cruzados: resultado.privilegios_cruzados,
            }
          )
        }
      } catch {
        // silencioso: un fallo en la actualizacion periodica no debe tirar
        // abajo la ruta/panel que ya estan mostrandose
      }
    }
    actualizar()
    const intervalo = setInterval(actualizar, TRAFICO_INTERVALO_MS)
    return () => {
      activo = false
      clearInterval(intervalo)
    }
  }, [])

  const handleSeleccionarOrigen = (id) => {
    setOrigenId(id)
    setRuta(null)
    setError(null)
    setDestinoId(null)
    destinoIdRef.current = null
  }

  const handleSeleccionarDestino = async (destino) => {
    setCargando(true)
    setError(null)
    setDestinoId(destino.id)
    destinoIdRef.current = destino.id
    try {
      const resultado = await calcularRutaDemo(origenId, destino.id)
      const puntos = resultado.geometry.coordinates.map(([lon, lat]) => [lat, lon])
      setRuta({
        puntos,
        distancia_m: resultado.distance_m,
        tiempo_s: resultado.time_s,
        tiempo_s_con_trafico: resultado.time_s_con_trafico,
        privilegios_cruzados: resultado.privilegios_cruzados,
      })
    } catch (e) {
      setError(e.message)
      setRuta(null)
      destinoIdRef.current = null
    } finally {
      setCargando(false)
    }
  }

  const origenActivo = ORIGENES.find((o) => o.id === origenId)
  const destinoActivo = DESTINOS.find((d) => d.id === destinoId)

  return (
    <div id="map-container">
      <div className="panel">
        <strong>APIO — demo</strong>
        <p className="panel-ayuda">
          Modo demo: rutas precalculadas (sin GraphHopper corriendo en vivo), pero tráfico y ETA son
          reales (TomTom). Elegí un punto de partida (🚑) y un hospital (🏥).
        </p>
        {cargando && <p>Calculando ruta...</p>}
        {error && <p className="panel-error">{error}</p>}
        {ruta && !cargando && (
          <p>
            <strong>
              {origenActivo?.nombre} → {destinoActivo?.nombre}
            </strong>
            <br />
            {(ruta.distancia_m / 1000).toFixed(2)} km
            <br />
            <span style={{ textDecoration: 'line-through', color: '#9ca3af' }}>
              {(ruta.tiempo_s / 60).toFixed(1)} min
            </span>{' '}
            → <strong>{(ruta.tiempo_s_con_trafico / 60).toFixed(1)} min</strong> con tráfico actual
            <span className="panel-ayuda"> (se actualiza solo)</span>
            {ruta.privilegios_cruzados?.length > 0 && (
              <>
                <br />
                <span className="panel-ayuda">
                  vía{' '}
                  {ruta.privilegios_cruzados
                    .map((p) => `${p.avenida} (${p.tipo_principal} ${p.tipo_principal_score}/10)`)
                    .join(', ')}
                </span>
              </>
            )}
          </p>
        )}
      </div>

      <div className="panel-trafico">
        <strong>Tráfico del corredor</strong>
        <ul>
          {trafico.map((t) => (
            <li key={t.avenida}>
              <span className="dot" style={{ background: NIVEL_COLORES[t.nivel] }} />
              {t.avenida}
            </li>
          ))}
        </ul>
      </div>

      <MapContainer center={CENTRO_AREQUIPA} zoom={13} style={{ height: '100%', width: '100%' }}>
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />

        {ORIGENES.map((origen) => (
          <Marker
            key={origen.id}
            position={origen.posicion}
            icon={origen.id === origenId ? iconoOrigenActivo : iconoOrigenInactivo}
            eventHandlers={{ click: () => handleSeleccionarOrigen(origen.id) }}
          >
            <Popup>
              <strong>{origen.nombre}</strong>
              <br />
              {origen.id === origenId ? 'Origen actual' : 'Clic para usar como origen'}
            </Popup>
          </Marker>
        ))}

        {DESTINOS.map((destino) => (
          <Marker
            key={destino.id}
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

export default DemoApp
