package pe.apio.mobile.datos

import pe.apio.mobile.modelo.EstablecimientoDto
import pe.apio.mobile.modelo.EstablecimientosResponse
import pe.apio.mobile.modelo.Hospital
import pe.apio.mobile.modelo.RecomendacionRequest
import pe.apio.mobile.modelo.RecomendacionResponse
import pe.apio.mobile.modelo.RouteResponse
import pe.apio.mobile.modelo.TrafficResponse
import pe.apio.mobile.modelo.Traslado
import pe.apio.mobile.red.ApioApi
import pe.apio.mobile.ui.EstadoRuta

/** Backend falso: cada respuesta se configura desde el test (o lanza el
 * error que se le indique, para simular "sin red"). */
class ApiFalsa : ApioApi {
    var establecimientos: List<EstablecimientoDto> = emptyList()
    var errorRed: Exception? = null
    var errorRuta: Exception? = null
    var llamadasEstablecimientos = 0

    override suspend fun obtenerRuta(origenLat: Double, origenLon: Double, destLat: Double, destLon: Double): RouteResponse {
        throw errorRuta ?: errorRed ?: IllegalStateException("ruta no configurada en el test")
    }

    override suspend fun obtenerTrafico(): TrafficResponse = TrafficResponse(avenidas = emptyList())

    override suspend fun obtenerEstablecimientos(): EstablecimientosResponse {
        llamadasEstablecimientos++
        errorRed?.let { throw it }
        return EstablecimientosResponse(total = establecimientos.size, establecimientos = establecimientos)
    }

    override suspend fun obtenerRecomendacionIa(datos: RecomendacionRequest): RecomendacionResponse {
        throw IllegalStateException("IA no disponible en tests")
    }
}

/** Cache en memoria que imita a Room (RoomRutaCacheStore). */
class CacheFalso : RutaCacheStore {
    val hospitales = mutableListOf<Hospital>()
    val rutas = mutableMapOf<String, EstadoRuta>()
    val traslados = mutableListOf<Traslado>()

    override suspend fun guardarHospitales(hospitales: List<Hospital>) {
        this.hospitales.clear()
        this.hospitales += hospitales
    }

    override suspend fun obtenerHospitalesCache(): List<Hospital> = hospitales.toList()

    override suspend fun guardarUltimaRuta(ruta: EstadoRuta) {
        rutas[ruta.destino.nombre] = ruta
    }

    override suspend fun obtenerUltimaRutaCache(destinoNombre: String): EstadoRuta? = rutas[destinoNombre]

    override suspend fun registrarTraslado(traslado: Traslado) {
        traslados += traslado
    }

    override suspend fun obtenerTraslados(limite: Int): List<Traslado> =
        traslados.sortedByDescending { it.fechaMs }.take(limite)
}
