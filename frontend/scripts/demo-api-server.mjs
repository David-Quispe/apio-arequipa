// Servidor minimo para probar en local las funciones serverless de la demo
// (frontend/api/*.js) sin tener que desplegar a Vercel para cada prueba.
// Uso: node scripts/demo-api-server.mjs (con TOMTOM_API_KEY en el entorno),
// despues levantar `npm run dev` -- vite.config.js redirige /api hacia este
// servidor solo en desarrollo (Vercel enruta /api por su cuenta en produccion).

import { createServer } from 'node:http'
import routeHandler from '../api/route.js'
import trafficHandler from '../api/traffic.js'

const PORT = 8787

function envolverRespuesta(res) {
  res.status = (code) => {
    res.statusCode = code
    return res
  }
  res.json = (body) => {
    res.setHeader('Content-Type', 'application/json')
    res.end(JSON.stringify(body))
  }
  return res
}

const servidor = createServer(async (req, res) => {
  const url = new URL(req.url, `http://${req.headers.host}`)
  req.query = Object.fromEntries(url.searchParams)
  envolverRespuesta(res)

  try {
    if (url.pathname === '/api/route') {
      await routeHandler(req, res)
    } else if (url.pathname === '/api/traffic') {
      await trafficHandler(req, res)
    } else {
      res.status(404).json({ detail: 'not found' })
    }
  } catch (e) {
    res.status(500).json({ detail: String(e) })
  }
})

servidor.listen(PORT, () => {
  console.log(`Demo API dev server en http://localhost:${PORT}`)
})
