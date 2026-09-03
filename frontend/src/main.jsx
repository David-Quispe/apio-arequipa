import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.jsx'
import DemoApp from './DemoApp.jsx'

// VITE_DEMO_MODE se define solo en el deploy de demo en Vercel (ver README,
// seccion "Demo sin servidor propio"). En desarrollo local no se define, asi
// que siempre corre la App real contra el backend/GraphHopper de verdad.
const Componente = import.meta.env.VITE_DEMO_MODE === 'true' ? DemoApp : App

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <Componente />
  </StrictMode>,
)
