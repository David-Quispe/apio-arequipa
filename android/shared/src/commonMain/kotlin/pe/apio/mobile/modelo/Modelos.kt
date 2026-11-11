package pe.apio.mobile.modelo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Modelos que calzan 1:1 con el contrato de /api/route y /api/traffic del
// backend (backend/app/api/routes/route.py y traffic.py) -- ver la memoria
// del proyecto (project_apio_rubrica.md) para el significado de cada campo.

@Serializable
data class Especificos(
    val contraflujo: Double? = null,
    @SerialName("carril_exclusivo") val carrilExclusivo: Double? = null,
    @SerialName("cruce_rojo") val cruceRojo: Double? = null,
)

@Serializable
data class PrivilegioCruzado(
    val avenida: String,
    val general: Double,
    val especificos: Especificos,
    @SerialName("tipo_principal") val tipoPrincipal: String? = null,
    @SerialName("tipo_principal_score") val tipoPrincipalScore: Double? = null,
)

@Serializable
data class Geometry(
    val type: String,
    // Pares [lon, lat] (orden GeoJSON) -- invertir a [lat, lon] antes de
    // usarlos en el mapa.
    val coordinates: List<List<Double>>,
)

@Serializable
data class RouteResponse(
    @SerialName("distance_m") val distanceM: Double,
    @SerialName("time_s") val timeS: Double,
    @SerialName("time_s_con_trafico") val timeSConTrafico: Double,
    @SerialName("privilegios_cruzados") val privilegiosCruzados: List<PrivilegioCruzado> = emptyList(),
    @SerialName("avenidas_trafico_cruzadas") val avenidasTraficoCruzadas: List<String> = emptyList(),
    val geometry: Geometry,
)

@Serializable
data class AvenidaTrafico(
    val avenida: String,
    @SerialName("current_speed_kmh") val currentSpeedKmh: Double? = null,
    @SerialName("free_flow_speed_kmh") val freeFlowSpeedKmh: Double? = null,
    val ratio: Double,
    val nivel: String,
)

@Serializable
data class TrafficResponse(
    val avenidas: List<AvenidaTrafico>,
)

@Serializable
data class ErrorResponse(
    val detail: String,
)

// ---- /api/establecimientos (backend/app/api/routes/establecimientos.py) ----

@Serializable
data class EstablecimientoDto(
    val codigo: String? = null,
    val nombre: String,
    val tipo: String = "Otro",
    val categoria: String? = null,
    val emergencia: Boolean = false,
    val distrito: String? = null,
    val telefono: String? = null,
    val lat: Double,
    val lon: Double,
)

@Serializable
data class EstablecimientosResponse(
    val total: Int = 0,
    val establecimientos: List<EstablecimientoDto> = emptyList(),
)

// ---- /api/ia/recomendacion (backend/app/api/routes/ia.py) ----

@Serializable
data class AvenidaNivel(
    val avenida: String,
    val nivel: String,
)

@Serializable
data class PrivilegioRuta(
    val avenida: String,
    val tipo: String? = null,
    val score: Double,
)

@Serializable
data class RecomendacionRequest(
    val destino: String,
    @SerialName("distancia_m") val distanciaM: Double,
    @SerialName("tiempo_s") val tiempoS: Double,
    @SerialName("tiempo_s_con_trafico") val tiempoSConTrafico: Double,
    @SerialName("avenidas_trafico") val avenidasTrafico: List<AvenidaNivel>,
    val privilegios: List<PrivilegioRuta>,
    @SerialName("riesgo_nivel") val riesgoNivel: String,
    @SerialName("riesgo_puntaje") val riesgoPuntaje: Double,
)

@Serializable
data class RecomendacionResponse(
    val texto: String,
    val modelo: String? = null,
)
