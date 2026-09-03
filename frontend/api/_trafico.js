// Puerto a JS de backend/app/services/traffic.py -- la funcion serverless de
// la demo (deploy en Vercel, ver README) no tiene acceso al backend Python,
// asi que este calculo pequeno (llamar a TomTom y convertir ratio -> factor
// de ETA) esta duplicado intencionalmente aca. Si se ajustan los umbrales
// (0.75/0.5) o el tope de factor_ajuste_eta en traffic.py, replicar el
// cambio aca tambien.

export const PUNTOS_AVENIDAS = {
  'Av. Ejercito': [-16.3895, -71.5478],
  'Puente Grau': [-16.3928, -71.539],
  'Av. Bolognesi': [-16.387, -71.538],
  'Av. Dolores': [-16.41157, -71.5285],
  'Av. Alcides Carrion': [-16.41211, -71.53549],
  'Av. Goyeneche': [-16.3992, -71.52535],
  Peral: [-16.39592, -71.53164],
  'El Filtro': [-16.39347, -71.53035],
}

const TOMTOM_URL = 'https://api.tomtom.com/traffic/services/4/flowSegmentData/absolute/10/json'
const CACHE_TTL_MS = 60_000

let cache = { timestamp: 0, data: null }

function nivel(ratio) {
  if (ratio >= 0.75) return 'baja'
  if (ratio >= 0.5) return 'media'
  return 'alta'
}

export function factorAjusteEta(ratio) {
  if (ratio <= 0) return 1.0
  return Math.max(1.0, Math.min(2.0, 1 / ratio))
}

async function consultarAvenida(nombre, [lat, lon], apiKey) {
  try {
    const url = `${TOMTOM_URL}?point=${lat},${lon}&key=${apiKey}`
    const resp = await fetch(url, { signal: AbortSignal.timeout(10_000) })
    if (!resp.ok) throw new Error(`TomTom respondio ${resp.status}`)
    const data = await resp.json()
    const flow = data.flowSegmentData
    const current = flow.currentSpeed
    const libre = flow.freeFlowSpeed
    const ratio = libre ? current / libre : 1.0
    return {
      avenida: nombre,
      current_speed_kmh: current,
      free_flow_speed_kmh: libre,
      ratio: Math.round(ratio * 100) / 100,
      nivel: nivel(ratio),
    }
  } catch (e) {
    return { avenida: nombre, error: String(e), ratio: 1.0, nivel: 'sin_datos' }
  }
}

export async function obtenerTraficoCorredor(apiKey) {
  const ahora = Date.now()
  if (cache.data && ahora - cache.timestamp < CACHE_TTL_MS) return cache.data

  const resultados = await Promise.all(
    Object.entries(PUNTOS_AVENIDAS).map(([nombre, punto]) => consultarAvenida(nombre, punto, apiKey))
  )
  cache = { timestamp: ahora, data: resultados }
  return resultados
}
