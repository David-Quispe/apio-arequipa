package pe.apio.mobile.red

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import pe.apio.mobile.modelo.ErrorResponse
import pe.apio.mobile.modelo.RouteResponse
import pe.apio.mobile.modelo.TrafficResponse

/** Se lanza cuando el backend responde con un error propio (400/502, con
 * {"detail": "..."}) en vez de un fallo de red -- ver route.py. */
class ApioApiException(message: String) : Exception(message)

/**
 * Cliente HTTP hacia el mismo backend FastAPI que ya usan la PWA y la demo
 * (ver backend/app/api/routes/route.py y traffic.py). No cambia nada del
 * backend -- esta app es un cliente mas.
 */
class ApioApiClient(private val baseUrl: String) {
    private val client = HttpClient(motorHttpCliente()) {
        expectSuccess = true
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun obtenerRuta(
        origenLat: Double,
        origenLon: Double,
        destLat: Double,
        destLon: Double,
    ): RouteResponse = manejarErrores {
        client.get("$baseUrl/route") {
            parameter("origin_lat", origenLat)
            parameter("origin_lon", origenLon)
            parameter("dest_lat", destLat)
            parameter("dest_lon", destLon)
        }.body()
    }

    suspend fun obtenerTrafico(): TrafficResponse = manejarErrores {
        client.get("$baseUrl/traffic").body()
    }

    private suspend fun <T> manejarErrores(bloque: suspend () -> T): T {
        try {
            return bloque()
        } catch (e: ClientRequestException) {
            throw ApioApiException(mensajeDeError(e) ?: "No se pudo calcular la ruta")
        } catch (e: ServerResponseException) {
            throw ApioApiException(mensajeDeError(e) ?: "El backend no pudo procesar la solicitud")
        }
    }

    private suspend fun mensajeDeError(e: Exception): String? = try {
        val respuesta = when (e) {
            is ClientRequestException -> e.response
            is ServerResponseException -> e.response
            else -> return null
        }
        respuesta.body<ErrorResponse>().detail
    } catch (_: Exception) {
        null
    }
}
