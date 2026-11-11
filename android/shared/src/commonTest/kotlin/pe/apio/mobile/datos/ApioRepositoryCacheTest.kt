package pe.apio.mobile.datos

import kotlinx.coroutines.test.runTest
import pe.apio.mobile.modelo.EstablecimientoDto
import pe.apio.mobile.modelo.HOSPITALES
import pe.apio.mobile.modelo.Hospital
import pe.apio.mobile.modelo.LatLon
import pe.apio.mobile.red.ApioApiException
import pe.apio.mobile.ui.EstadoRuta
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

/** Prueba como el repositorio decide entre el servidor y el cache local
 * (Room en la app real) sin red ni base de datos de verdad. */
class ApioRepositoryCacheTest {

    private val api = ApiFalsa()
    private val cache = CacheFalso()
    private val repositorio = ApioRepository(api, cache)

    private val honorio = EstablecimientoDto(
        codigo = "00001", nombre = "Hospital Honorio Delgado", tipo = "MINSA",
        categoria = "III-1", emergencia = true, lat = -16.40, lon = -71.53,
    )

    @Test
    fun hospitalesDelServidorSeGuardanEnCache() = runTest {
        api.establecimientos = listOf(honorio)

        val resultado = repositorio.obtenerHospitales()

        assertEquals(OrigenDatos.SERVIDOR, resultado.origen)
        assertEquals(listOf("Hospital Honorio Delgado"), resultado.hospitales.map { it.nombre })
        assertEquals(1, cache.hospitales.size)
    }

    @Test
    fun sinRedUsaLosHospitalesGuardados() = runTest {
        cache.hospitales += Hospital("Clinica Arequipa", "Privada", LatLon(-16.39, -71.54))
        api.errorRed = RuntimeException("sin internet")

        val resultado = repositorio.obtenerHospitales()

        assertEquals(OrigenDatos.CACHE, resultado.origen)
        assertEquals("Clinica Arequipa", resultado.hospitales.single().nombre)
    }

    @Test
    fun sinRedNiCacheUsaLaListaDeRespaldo() = runTest {
        api.errorRed = RuntimeException("sin internet")

        val resultado = repositorio.obtenerHospitales()

        assertEquals(OrigenDatos.RESPALDO, resultado.origen)
        assertEquals(HOSPITALES, resultado.hospitales)
    }

    @Test
    fun sinRedDevuelveLaUltimaRutaGuardada() = runTest {
        val destino = Hospital("Hospital Goyeneche", "MINSA", LatLon(-16.39, -71.53))
        val guardada = EstadoRuta(destino, 5000.0, 600.0, 700.0, emptyList(), emptyList(), emptyList())
        cache.rutas[destino.nombre] = guardada
        api.errorRed = RuntimeException("sin internet")

        val ruta = repositorio.obtenerRuta(LatLon(-16.38, -71.55), destino)

        assertSame(guardada, ruta)
    }

    @Test
    fun errorDelBackendNoSeTapaConElCache() = runTest {
        val destino = Hospital("Hospital Goyeneche", "MINSA", LatLon(-16.39, -71.53))
        cache.rutas[destino.nombre] = EstadoRuta(destino, 1.0, 1.0, 1.0, emptyList(), emptyList(), emptyList())
        api.errorRuta = ApioApiException("Origen fuera del area piloto")

        val error = assertFailsWith<ApioApiException> {
            repositorio.obtenerRuta(LatLon(-15.0, -70.0), destino)
        }
        assertEquals("Origen fuera del area piloto", error.message)
    }
}
