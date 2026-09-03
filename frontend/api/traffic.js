// Funcion serverless de Vercel -- espejo minimo de GET /api/traffic del
// backend real (backend/app/api/routes/traffic.py), para el modo demo (ver
// README, seccion "Demo sin servidor propio").

import { obtenerTraficoCorredor } from './_trafico.js'

export default async function handler(req, res) {
  const avenidas = await obtenerTraficoCorredor(process.env.TOMTOM_API_KEY)
  res.status(200).json({ avenidas })
}
