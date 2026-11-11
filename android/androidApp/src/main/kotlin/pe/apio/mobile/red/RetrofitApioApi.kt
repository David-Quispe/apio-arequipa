package pe.apio.mobile.red

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import pe.apio.mobile.modelo.ErrorResponse
import pe.apio.mobile.modelo.EstablecimientosResponse
import pe.apio.mobile.modelo.RecomendacionRequest
import pe.apio.mobile.modelo.RecomendacionResponse
import pe.apio.mobile.modelo.RouteResponse
import pe.apio.mobile.modelo.TrafficResponse
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Implementacion Android de [ApioApi] con Retrofit + OkHttp. Convierte el
 * JSON con kotlinx.serialization (los mismos modelos @Serializable del
 * modulo shared) y traduce los errores del backend ({"detail": "..."}) a
 * [ApioApiException], igual que el cliente Ktor, para que el repositorio
 * los trate de la misma forma.
 */
class RetrofitApioApi(baseUrl: String) : ApioApi {

    private val json = Json { ignoreUnknownKeys = true }

    private val okHttp = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        // la recomendacion de Gemini puede tardar unos segundos
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val servicio: ApioRetrofitService = Retrofit.Builder()
        // Retrofit exige que la base URL termine en "/"
        .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
        .client(okHttp)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(ApioRetrofitService::class.java)

    override suspend fun obtenerRuta(
        origenLat: Double,
        origenLon: Double,
        destLat: Double,
        destLon: Double,
    ): RouteResponse = manejarErrores("No se pudo calcular la ruta") {
        servicio.obtenerRuta(origenLat, origenLon, destLat, destLon)
    }

    override suspend fun obtenerTrafico(): TrafficResponse =
        manejarErrores("No se pudo obtener el trafico") { servicio.obtenerTrafico() }

    override suspend fun obtenerEstablecimientos(): EstablecimientosResponse =
        manejarErrores("No se pudo obtener los establecimientos") { servicio.obtenerEstablecimientos() }

    override suspend fun obtenerRecomendacionIa(datos: RecomendacionRequest): RecomendacionResponse =
        manejarErrores("La IA no esta disponible") { servicio.obtenerRecomendacionIa(datos) }

    override fun cerrar() {
        okHttp.dispatcher.executorService.shutdown()
        okHttp.connectionPool.evictAll()
    }

    /** Un HTTP 4xx/5xx del backend se convierte en [ApioApiException] con su
     * "detail"; los fallos de red (sin internet, timeout) se dejan pasar
     * para que el repositorio use el cache de Room. */
    private suspend fun <T> manejarErrores(mensajePorDefecto: String, bloque: suspend () -> T): T {
        try {
            return bloque()
        } catch (e: HttpException) {
            val detalle = try {
                e.response()?.errorBody()?.string()?.let {
                    json.decodeFromString(ErrorResponse.serializer(), it).detail
                }
            } catch (_: Exception) {
                null
            }
            throw ApioApiException(detalle ?: mensajePorDefecto)
        }
    }
}
