package pe.apio.mobile.mapa

import pe.apio.mobile.modelo.LatLon
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ModeloAmbulanciaTest {

    private val plaza = LatLon(-16.3989, -71.5369)

    private fun largoNorteSurM(piezas: List<PiezaAmbulancia>): Double {
        val lats = piezas.flatMap { it.contorno }.map { it.lat }
        return (lats.max() - lats.min()) * 110_540.0
    }

    private fun anchoEsteOesteM(piezas: List<PiezaAmbulancia>): Double {
        val lons = piezas.flatMap { it.contorno }.map { it.lon }
        return (lons.max() - lons.min()) * 111_320.0 * kotlin.math.cos(plaza.lat * kotlin.math.PI / 180)
    }

    @Test
    fun apuntandoAlNorteElLargoVaDeNorteASur() {
        val piezas = modeloAmbulancia(plaza, rumbo = 0f, largoM = 60.0)
        assertTrue(abs(largoNorteSurM(piezas) - 60.6) < 1.0, "largo ${largoNorteSurM(piezas)}")
        assertTrue(anchoEsteOesteM(piezas) < 30.0)
    }

    @Test
    fun apuntandoAlEsteElLargoVaDeEsteAOeste() {
        val piezas = modeloAmbulancia(plaza, rumbo = 90f, largoM = 60.0)
        assertTrue(abs(anchoEsteOesteM(piezas) - 60.6) < 1.0, "largo ${anchoEsteOesteM(piezas)}")
        assertTrue(largoNorteSurM(piezas) < 30.0)
    }

    @Test
    fun lasAlturasSeEscalanConElTamano() {
        val chico = modeloAmbulancia(plaza, 0f, largoM = 6.0)
        val grande = modeloAmbulancia(plaza, 0f, largoM = 60.0)
        assertEquals(chico.size, grande.size)
        assertTrue(abs(grande.maxOf { it.altoM } - 10 * chico.maxOf { it.altoM }) < 1e-6)
        assertTrue(chico.all { it.contorno.size == 4 && it.altoM > it.baseM })
    }

    @Test
    fun cadaNivelDeZoomDuplicaLaEscala() {
        val a = metrosPorPixel(plaza.lat, 16.0)
        val b = metrosPorPixel(plaza.lat, 17.0)
        assertTrue(abs(a / b - 2.0) < 1e-9)
    }
}
