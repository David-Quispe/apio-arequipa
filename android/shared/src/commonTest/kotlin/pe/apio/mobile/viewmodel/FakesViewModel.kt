package pe.apio.mobile.viewmodel

import pe.apio.mobile.datos.RutaCacheStore
import pe.apio.mobile.modelo.AvenidaTrafico
import pe.apio.mobile.modelo.EstablecimientoDto
import pe.apio.mobile.modelo.EstablecimientosResponse
import pe.apio.mobile.modelo.Geometry
import pe.apio.mobile.modelo.Hospital
import pe.apio.mobile.modelo.LatLon
import pe.apio.mobile.modelo.RecomendacionRequest
import pe.apio.mobile.modelo.RecomendacionResponse
import pe.apio.mobile.modelo.RouteResponse
import pe.apio.mobile.modelo.TrafficResponse
import pe.apio.mobile.modelo.Traslado
import pe.apio.mobile.red.ApioApi
import pe.apio.mobile.ui.EstadoRuta

val HOSPITAL_PRUEBA = Hospital("Hospital de Prueba", "MINSA", LatLon(-16.40, -71.53))

/** Backend falso programable: cada llamada devuelve lo configurado o lanza
 * el error indicado, para simular "sin red", "IA caida", etc. */
class ApiProgramable : ApioApi {
    var respuestaRuta: RouteResponse = RouteResponse(
        distanceM = 5000.0,
        timeS = 600.0,
        timeSConTrafico = 700.0,
        geometry = Geometry("LineString", listOf(listOf(-71.55, -16.38), listOf(-71.53, -16.40))),
    )
    var errorRuta: Exception? = null
    var trafico: List<AvenidaTrafico> = emptyList()
    var errorTrafico: Exception? = null
    var errorIa: Exception? = null
    var textoIa: String = "Tome Av. Ejercito"
    var establecimientos: List<EstablecimientoDto> = emptyList()
    var errorEstablecimientos: Exception? = null

    var llamadasRuta = 0
    var llamadasIa = 0

    override suspend fun obtenerRuta(origenLat: Double, origenLon: Double, destLat: Double, destLon: Double): RouteResponse {
        llamadasRuta++
        errorRuta?.let { throw it }
        return respuestaRuta
    }

    override suspend fun obtenerTrafico(): TrafficResponse {
        errorTrafico?.let { throw it }
        return TrafficResponse(avenidas = trafico)
    }

    override suspend fun obtenerEstablecimientos(): EstablecimientosResponse {
        errorEstablecimientos?.let { throw it }
        return EstablecimientosResponse(total = establecimientos.size, establecimientos = establecimientos)
    }

    override suspend fun obtenerRecomendacionIa(datos: RecomendacionRequest): RecomendacionResponse {
        llamadasIa++
        errorIa?.let { throw it }
        return RecomendacionResponse(texto = textoIa)
    }
}

/** Cache cuyo almacenamiento siempre falla: simula Room roto o sin espacio. */
class CacheRoto : RutaCacheStore {
    override suspend fun guardarHospitales(hospitales: List<Hospital>) = throw IllegalStateException("Room roto")
    override suspend fun obtenerHospitalesCache(): List<Hospital> = throw IllegalStateException("Room roto")
    override suspend fun guardarUltimaRuta(ruta: EstadoRuta) = throw IllegalStateException("Room roto")
    override suspend fun obtenerUltimaRutaCache(destinoNombre: String): EstadoRuta? = throw IllegalStateException("Room roto")
    override suspend fun registrarTraslado(traslado: Traslado) = throw IllegalStateException("Room roto")
    override suspend fun obtenerTraslados(limite: Int): List<Traslado> = throw IllegalStateException("Room roto")
}
