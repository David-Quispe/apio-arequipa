// Funcion serverless de Vercel para el modo demo (ver README, seccion "Demo
// sin servidor propio"). No hay GraphHopper/PostGIS corriendo detras: la
// distancia, la geometria y los metros que la ruta superpone con cada
// avenida del corredor vienen precalculados en src/demo/rutas.json (contra
// el GraphHopper/PostGIS reales -- ver backend/scripts/generar_demo_rutas.py).
// Lo unico que esta funcion calcula en el momento es el ajuste de ETA por
// trafico en vivo (TomTom), con la misma formula que
// backend/app/api/routes/route.py.

import { readFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { factorAjusteEta, obtenerTraficoCorredor } from './_trafico.js'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const rutasData = JSON.parse(readFileSync(path.join(__dirname, '..', 'src', 'demo', 'rutas.json'), 'utf-8'))

export default async function handler(req, res) {
  const { origen, destino } = req.query
  const ruta = rutasData.rutas[`${origen}|${destino}`]
  if (!ruta) {
    res.status(400).json({ detail: 'Esa combinación de origen/destino no está disponible en el modo demo.' })
    return
  }

  let tiempoConTrafico = ruta.time_s
  try {
    const trafico = await obtenerTraficoCorredor(process.env.TOMTOM_API_KEY)
    const ratioPorAvenida = Object.fromEntries(trafico.map((t) => [t.avenida, t.ratio]))
    const metros = ruta.metros_por_avenida
    const totalMetros = Object.values(metros).reduce((a, b) => a + b, 0)
    if (totalMetros > 0) {
      const ratioPonderado =
        Object.entries(metros).reduce((acc, [avenida, m]) => acc + m * (ratioPorAvenida[avenida] ?? 1.0), 0) /
        totalMetros
      tiempoConTrafico = ruta.time_s * factorAjusteEta(ratioPonderado)
    }
  } catch {
    // sin datos de trafico: se devuelve el ETA base sin ajustar
  }

  res.status(200).json({
    distance_m: ruta.distance_m,
    time_s: ruta.time_s,
    time_s_con_trafico: Math.round(tiempoConTrafico * 10) / 10,
    privilegios_cruzados: ruta.privilegios_cruzados,
    geometry: ruta.geometry,
  })
}
