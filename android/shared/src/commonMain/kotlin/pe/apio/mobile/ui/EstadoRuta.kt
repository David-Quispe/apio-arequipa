package pe.apio.mobile.ui

import pe.apio.mobile.modelo.LatLon
import pe.apio.mobile.modelo.PrivilegioCruzado

/** Lo que necesita el panel de info para dibujarse, ya combinado con el
 * nombre del hospital (que no viene en la respuesta del backend). */
data class EstadoRuta(
    val destinoNombre: String,
    val distanciaM: Double,
    val tiempoS: Double,
    val tiempoSConTrafico: Double,
    val privilegiosCruzados: List<PrivilegioCruzado>,
    val puntos: List<LatLon>,
)
