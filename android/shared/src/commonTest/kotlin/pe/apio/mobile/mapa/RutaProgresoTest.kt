package pe.apio.mobile.mapa

import pe.apio.mobile.modelo.LatLon
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RutaProgresoTest {

    // tres puntos hacia el este sobre la misma latitud (~107 m entre cada uno)
    private val ruta = listOf(
        LatLon(-16.4000, -71.5400),
        LatLon(-16.4000, -71.5390),
        LatLon(-16.4000, -71.5380),
    )

    @Test
    fun pegaLaPosicionALaCalle() {
        // ~10 m al norte de la mitad del segundo tramo
        val gps = LatLon(-16.4000 + 10.0 / 110_540.0, -71.5385)
        val p = proyectarEnRuta(ruta, gps)!!
        assertEquals(1, p.segmento)
        assertTrue(abs(p.distanciaM - 10.0) < 0.2, "distancia ${p.distanciaM}")
        assertTrue(abs(p.punto.lat - (-16.4000)) < 1e-7)
        assertTrue(abs(p.punto.lon - (-71.5385)) < 1e-7)
    }

    @Test
    fun antesDelInicioSeQuedaEnElPrimerPunto() {
        val p = proyectarEnRuta(ruta, LatLon(-16.4000, -71.5410))!!
        assertEquals(0, p.segmento)
        assertTrue(abs(p.punto.lon - (-71.5400)) < 1e-9)
    }

    @Test
    fun laRutaRestanteEmpiezaEnLaProyeccion() {
        val p = proyectarEnRuta(ruta, LatLon(-16.4000, -71.5395))!!
        val restante = rutaRestante(ruta, p)
        assertEquals(3, restante.size)
        assertEquals(p.punto, restante.first())
        assertEquals(ruta.last(), restante.last())
    }

    @Test
    fun sinTramosNoHayProyeccion() {
        assertNull(proyectarEnRuta(listOf(ruta.first()), ruta.first()))
    }
}
