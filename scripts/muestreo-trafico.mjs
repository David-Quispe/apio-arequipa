// Registra una muestra del trafico en vivo del corredor, consultando la demo
// ya desplegada en Vercel (no el backend local -- asi la recoleccion no
// depende de que la laptop de David este prendida). Pensado para correr
// periodicamente desde .github/workflows/muestreo-trafico.yml y acumular,
// a lo largo de dias/semanas, una medicion real de congestion por avenida
// en vez de la observacion puntual de la rubrica de privilegios (ver
// backend/scripts/actualizar_congestion_desde_historico.py).

import { appendFileSync, mkdirSync } from 'node:fs'

const DEMO_URL = 'https://apio-arequipa.vercel.app/api/traffic'
const SALIDA = 'data/trafico_historico.jsonl'

const resp = await fetch(DEMO_URL)
if (!resp.ok) {
  console.error(`No se pudo obtener trafico de la demo: HTTP ${resp.status}`)
  process.exit(1)
}
const data = await resp.json()

mkdirSync('data', { recursive: true })
const registro = { timestamp: new Date().toISOString(), avenidas: data.avenidas }
appendFileSync(SALIDA, `${JSON.stringify(registro)}\n`)

console.log(`Muestra registrada (${registro.timestamp}): ${registro.avenidas.length} avenidas.`)
