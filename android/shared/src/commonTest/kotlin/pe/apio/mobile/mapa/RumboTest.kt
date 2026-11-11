package pe.apio.mobile.mapa

import pe.apio.mobile.modelo.LatLon
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RumboTest {

    private val plaza = LatLon(-16.3989, -71.5369)

    private fun assertCerca(esperado: Float, real: Float, tolerancia: Float = 0.5f) {
        val diferencia = abs(((real - esperado + 540f) % 360f) - 180f)
        assertTrue(diferencia <= tolerancia, "esperado $esperado, real $real")
    }

    @Test
    fun rumboHaciaLosCuatroPuntosCardinales() {
        assertCerca(0f, rumboEntre(plaza, LatLon(plaza.lat + 0.01, plaza.lon)))
        assertCerca(90f, rumboEntre(plaza, LatLon(plaza.lat, plaza.lon + 0.01)))
        assertCerca(180f, rumboEntre(plaza, LatLon(plaza.lat - 0.01, plaza.lon)))
        assertCerca(270f, rumboEntre(plaza, LatLon(plaza.lat, plaza.lon - 0.01)))
    }

    @Test
    fun interpolaPorElLadoMasCorto() {
        // de 350 a 10 son 20 grados pasando por el norte, no 340 por el sur
        assertCerca(0f, interpolarRumbo(350f, 10f, 0.5f))
        assertCerca(355f, interpolarRumbo(350f, 10f, 0.25f))
        assertCerca(350f, interpolarRumbo(10f, 350f, 1f))
        assertCerca(45f, interpolarRumbo(0f, 90f, 0.5f))
    }

    @Test
    fun normalizaAngulosFueraDeRango() {
        assertEquals(350f, normalizarRumbo(-10f))
        assertEquals(10f, normalizarRumbo(370f))
        assertEquals(0f, normalizarRumbo(360f))
    }

    @Test
    fun detenidaNoGiraAunqueElGpsDigaOtroRumbo() {
        val nuevo = LatLon(plaza.lat + 0.0001, plaza.lon)
        assertEquals(120f, siguienteRumbo(plaza, nuevo, rumboActual = 120f, rumboGps = 300f, velocidadMs = 0.2f))
    }

    @Test
    fun enMovimientoPrefiereElRumboDelGps() {
        val nuevo = LatLon(plaza.lat + 0.001, plaza.lon)
        assertEquals(45f, siguienteRumbo(plaza, nuevo, rumboActual = 120f, rumboGps = 45f, velocidadMs = 8f))
    }

    @Test
    fun sinRumboDelGpsLoCalculaSoloSiAvanzoLoSuficiente() {
        // ~2 m al este: ruido, conserva el rumbo
        val ruido = LatLon(plaza.lat, plaza.lon + 0.00002)
        assertEquals(120f, siguienteRumbo(plaza, ruido, rumboActual = 120f))
        // ~107 m al este: rumbo calculado, cerca de 90
        val avance = LatLon(plaza.lat, plaza.lon + 0.001)
        assertCerca(90f, siguienteRumbo(plaza, avance, rumboActual = 120f))
    }
}
