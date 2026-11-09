package pe.apio.mobile.datos

import pe.apio.mobile.modelo.AvenidaTrafico
import pe.apio.mobile.modelo.Hospital
import pe.apio.mobile.modelo.LatLon
import pe.apio.mobile.red.ApioApiClient
import pe.apio.mobile.red.ApioApiException
import pe.apio.mobile.ui.EstadoRuta

/**
 * Capa de datos del patron MVVM: el ViewModel pide aca lo que necesita y no
 * sabe nada de Ktor, de JSON ni del orden [lon, lat] de GeoJSON. [cache] es
 * opcional (null en los @Preview y en tests): cuando esta presente, cada
 * ruta calculada con exito se guarda y, si la red falla por un motivo que
 * no es un error propio del backend ([ApioApiException] -- ej. "fuera del
 * area piloto"), se devuelve la ultima ruta guardada para ese mismo destino
 * en vez de romper la pantalla.
 */
class ApioRepository(
    private val api: ApioApiClient,
    private val cache: RutaCacheStore? = null,
) {

    suspend fun obtenerRuta(origen: LatLon, destino: Hospital): EstadoRuta {
        try {
            val respuesta = api.obtenerRuta(
                origenLat = origen.lat,
                origenLon = origen.lon,
                destLat = destino.posicion.lat,
                destLon = destino.posicion.lon,
            )
            val ruta = EstadoRuta(
                destino = destino,
                distanciaM = respuesta.distanceM,
                tiempoS = respuesta.timeS,
                tiempoSConTrafico = respuesta.timeSConTrafico,
                privilegiosCruzados = respuesta.privilegiosCruzados,
                avenidasTraficoCruzadas = respuesta.avenidasTraficoCruzadas,
                // GeoJSON viene como [lon, lat]; osmdroid necesita [lat, lon].
                puntos = respuesta.geometry.coordinates.map { LatLon(it[1], it[0]) },
            )
            cache?.guardarUltimaRuta(ruta)
            return ruta
        } catch (e: ApioApiException) {
            // error propio del backend (ej. fuera del area piloto): una
            // ruta vieja en cache no arregla eso, hay que mostrar el error.
            throw e
        } catch (e: Exception) {
            val cacheada = cache?.obtenerUltimaRutaCache(destino.nombre)
            if (cacheada != null) return cacheada
            throw e
        }
    }

    suspend fun obtenerTrafico(): List<AvenidaTrafico> = api.obtenerTrafico().avenidas

    /** Escribe el listado de hospitales en el cache local. Hoy [Hospital]
     * sale de la constante hardcodeada HOSPITALES (no hay endpoint de red
     * todavia), pero dejar esto conectado hace trivial el dia que haya uno. */
    suspend fun sincronizarHospitalesCache(hospitales: List<Hospital>) {
        cache?.guardarHospitales(hospitales)
    }

    fun cerrar() = api.cerrar()
}
