package pe.apio.mobile.mapa

import pe.apio.mobile.modelo.LatLon
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sqrt

/**
 * El punto de la ruta mas cercano a una posicion: [punto] sobre la linea,
 * [segmento] = indice del tramo donde cae (entre ruta[segmento] y
 * ruta[segmento + 1]) y [distanciaM] = que tan lejos estaba la posicion.
 */
data class ProyeccionRuta(val punto: LatLon, val segmento: Int, val distanciaM: Double)

/**
 * Proyecta [posicion] sobre la polilinea [ruta]. Sirve para dos cosas del
 * modo navegacion: pegar la ambulancia a la calle (el GPS tiembla unos
 * metros) y saber que parte de la ruta ya quedo atras. Devuelve null si la
 * ruta no tiene al menos un tramo.
 */
fun proyectarEnRuta(ruta: List<LatLon>, posicion: LatLon): ProyeccionRuta? {
    if (ruta.size < 2) return null
    // plano local en metros con origen en la posicion (sobra para una ciudad)
    val mLon = 111_320.0 * cos(posicion.lat * PI / 180)
    val mLat = 110_540.0
    var mejor: ProyeccionRuta? = null
    for (i in 0 until ruta.size - 1) {
        val ax = (ruta[i].lon - posicion.lon) * mLon
        val ay = (ruta[i].lat - posicion.lat) * mLat
        val dx = (ruta[i + 1].lon - posicion.lon) * mLon - ax
        val dy = (ruta[i + 1].lat - posicion.lat) * mLat - ay
        val largo2 = dx * dx + dy * dy
        // t = en que fraccion del tramo cae la perpendicular (0 = inicio, 1 = fin)
        val t = if (largo2 == 0.0) 0.0 else (-(ax * dx + ay * dy) / largo2).coerceIn(0.0, 1.0)
        val qx = ax + t * dx
        val qy = ay + t * dy
        val distancia = sqrt(qx * qx + qy * qy)
        if (mejor == null || distancia < mejor.distanciaM) {
            mejor = ProyeccionRuta(LatLon(posicion.lat + qy / mLat, posicion.lon + qx / mLon), i, distancia)
        }
    }
    return mejor
}

/** Lo que falta recorrer: desde el punto proyectado hasta el final de la ruta. */
fun rutaRestante(ruta: List<LatLon>, proyeccion: ProyeccionRuta): List<LatLon> =
    listOf(proyeccion.punto) + ruta.drop(proyeccion.segmento + 1)
