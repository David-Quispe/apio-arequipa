package pe.apio.mobile.red

import pe.apio.mobile.modelo.EstablecimientosResponse
import pe.apio.mobile.modelo.RecomendacionRequest
import pe.apio.mobile.modelo.RecomendacionResponse
import pe.apio.mobile.modelo.RouteResponse
import pe.apio.mobile.modelo.TrafficResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Endpoints del backend FastAPI (backend/app/api/routes) declarados para
 * Retrofit. Las rutas son relativas a la base URL ".../api/".
 */
interface ApioRetrofitService {

    @GET("route")
    suspend fun obtenerRuta(
        @Query("origin_lat") origenLat: Double,
        @Query("origin_lon") origenLon: Double,
        @Query("dest_lat") destLat: Double,
        @Query("dest_lon") destLon: Double,
    ): RouteResponse

    @GET("traffic")
    suspend fun obtenerTrafico(): TrafficResponse

    @GET("establecimientos")
    suspend fun obtenerEstablecimientos(): EstablecimientosResponse

    @POST("ia/recomendacion")
    suspend fun obtenerRecomendacionIa(@Body datos: RecomendacionRequest): RecomendacionResponse
}
