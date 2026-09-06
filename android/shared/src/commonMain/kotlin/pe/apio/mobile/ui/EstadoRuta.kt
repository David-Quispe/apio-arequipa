package pe.apio.mobile.ui

import pe.apio.mobile.modelo.Hospital
import pe.apio.mobile.modelo.LatLon
import pe.apio.mobile.modelo.PrivilegioCruzado

/** Lo que necesita el panel de info para dibujarse. Guarda el hospital
 * completo (no solo el nombre) porque el auto-refresh del ETA cada 25s
 * necesita sus coordenadas para volver a pedir la ruta. */
data class EstadoRuta(
    val destino: Hospital,
    val distanciaM: Double,
    val tiempoS: Double,
    val tiempoSConTrafico: Double,
    val privilegiosCruzados: List<PrivilegioCruzado>,
    val puntos: List<LatLon>,
)
