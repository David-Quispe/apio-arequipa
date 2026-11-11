package pe.apio.mobile.mapa

import pe.apio.mobile.modelo.LatLon
import pe.apio.mobile.ui.distanciaLineaRectaM
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

// Por debajo de esta velocidad se considera que la ambulancia esta detenida:
// el GPS igual "tiembla" unos metros y no debe hacerla girar sola.
private const val VELOCIDAD_QUIETO_MS = 1.0f

// Sin rumbo del GPS, hace falta avanzar al menos esto para confiar en el
// rumbo calculado entre dos posiciones (menos que eso es ruido).
private const val AVANCE_MINIMO_M = 8.0

/** Lleva cualquier angulo al rango [0, 360). */
fun normalizarRumbo(grados: Float): Float = ((grados % 360f) + 360f) % 360f

/**
 * Rumbo (azimut) en grados para ir de [desde] hacia [hasta]: 0 = norte,
 * 90 = este, 180 = sur, 270 = oeste. Formula del azimut inicial sobre la
 * esfera; para los tramos cortos de una ciudad es exacta de sobra.
 */
fun rumboEntre(desde: LatLon, hasta: LatLon): Float {
    val lat1 = desde.lat * PI / 180
    val lat2 = hasta.lat * PI / 180
    val deltaLon = (hasta.lon - desde.lon) * PI / 180
    val y = sin(deltaLon) * cos(lat2)
    val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(deltaLon)
    return normalizarRumbo((atan2(y, x) * 180 / PI).toFloat())
}

/**
 * Punto intermedio entre dos rumbos girando por el lado mas corto: de 350 a
 * 10 pasa por 0 (20 grados de giro), no da la vuelta larga de 340.
 * [fraccion] va de 0 (rumbo [desde]) a 1 (rumbo [hasta]).
 */
fun interpolarRumbo(desde: Float, hasta: Float, fraccion: Float): Float {
    val giro = ((hasta - desde + 540f) % 360f) - 180f
    return normalizarRumbo(desde + giro * fraccion)
}

/**
 * Decide hacia donde debe apuntar la ambulancia despues de una lectura
 * nueva de ubicacion:
 *  - detenida (velocidad casi 0): conserva el rumbo que tenia;
 *  - el GPS informa rumbo: se usa ese, es el mas fiable en movimiento;
 *  - si no, se calcula entre la posicion anterior y la nueva, pero solo si
 *    avanzo lo suficiente como para que no sea ruido.
 */
fun siguienteRumbo(
    anterior: LatLon,
    nuevo: LatLon,
    rumboActual: Float,
    rumboGps: Float? = null,
    velocidadMs: Float? = null,
): Float {
    if (velocidadMs != null && velocidadMs < VELOCIDAD_QUIETO_MS) return rumboActual
    if (rumboGps != null) return normalizarRumbo(rumboGps)
    if (distanciaLineaRectaM(anterior, nuevo) < AVANCE_MINIMO_M) return rumboActual
    return rumboEntre(anterior, nuevo)
}
