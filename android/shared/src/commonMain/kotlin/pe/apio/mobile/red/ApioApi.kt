package pe.apio.mobile.red

import pe.apio.mobile.modelo.EstablecimientosResponse
import pe.apio.mobile.modelo.RecomendacionRequest
import pe.apio.mobile.modelo.RecomendacionResponse
import pe.apio.mobile.modelo.RouteResponse
import pe.apio.mobile.modelo.TrafficResponse

/**
 * Contrato de red de la app: lo que [pe.apio.mobile.datos.ApioRepository]
 * necesita del backend FastAPI. Hay dos implementaciones:
 *
 * - Android: RetrofitApioApi (androidApp/.../red), con Retrofit + OkHttp.
 *   Es la que usa la app real (MainActivity).
 * - Comun: [ApioApiClient] con Ktor, para los @Preview y para sumar iOS
 *   despues sin reescribir la logica.
 *
 * Asi el repositorio y el ViewModel no saben que libreria HTTP hay debajo.
 */
interface ApioApi {
    suspend fun obtenerRuta(
        origenLat: Double,
        origenLon: Double,
        destLat: Double,
        destLon: Double,
    ): RouteResponse

    suspend fun obtenerTrafico(): TrafficResponse

    /** Todos los establecimientos de salud de Arequipa (RENIPRESS + OSM). */
    suspend fun obtenerEstablecimientos(): EstablecimientosResponse

    /** Recomendacion del LLM (Gemini) generada en el backend. */
    suspend fun obtenerRecomendacionIa(datos: RecomendacionRequest): RecomendacionResponse

    /** Libera recursos de red; lo llama el ViewModel en onCleared(). */
    fun cerrar() {}
}
