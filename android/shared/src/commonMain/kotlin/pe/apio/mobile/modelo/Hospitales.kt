package pe.apio.mobile.modelo

// Coordenadas verificadas contra el poligono del edificio en OpenStreetMap
// (Overpass API), igual que frontend/src/App.jsx -- no geocodificacion
// aproximada. Mantener esta lista sincronizada con la de ahi.
val HOSPITALES = listOf(
    Hospital(
        nombre = "Hospital Regional Honorio Delgado Espinoza",
        tipo = "MINSA",
        posicion = LatLon(-16.4149129, -71.5315582),
    ),
    Hospital(
        nombre = "Hospital Goyeneche",
        tipo = "MINSA",
        posicion = LatLon(-16.4024925, -71.5279176),
    ),
    Hospital(
        nombre = "Hospital Nacional Carlos Alberto Seguín Escobedo",
        tipo = "EsSalud",
        posicion = LatLon(-16.3952624, -71.5307408),
    ),
    Hospital(
        nombre = "Clínica Arequipa",
        tipo = "Privada",
        posicion = LatLon(-16.391834, -71.5400938),
    ),
)

// Mismos colores que TIPO_COLORES en frontend/src/App.jsx.
val COLOR_POR_TIPO = mapOf(
    "MINSA" to "#2563eb",
    "EsSalud" to "#059669",
    "Privada" to "#7c3aed",
)

const val COLOR_AMBULANCIA = "#111827"
