package pe.apio.mobile.datos

import pe.apio.mobile.ia.EvaluacionRiesgo
import pe.apio.mobile.modelo.AvenidaNivel
import pe.apio.mobile.modelo.AvenidaTrafico
import pe.apio.mobile.modelo.EstablecimientoDto
import pe.apio.mobile.modelo.HOSPITALES
import pe.apio.mobile.modelo.Hospital
import pe.apio.mobile.modelo.LatLon
import pe.apio.mobile.modelo.PrivilegioRuta
import pe.apio.mobile.modelo.RecomendacionRequest
import pe.apio.mobile.modelo.Traslado
import pe.apio.mobile.red.ApioApi
import pe.apio.mobile.red.ApioApiException
import pe.apio.mobile.ui.EstadoRuta
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Capa de datos del patron MVVM: el ViewModel pide aca lo que necesita y no
 * sabe nada de Retrofit/Ktor, de JSON ni del orden [lon, lat] de GeoJSON. [cache] es
 * opcional (null en los @Preview y en tests): cuando esta presente, cada
 * ruta calculada con exito se guarda y, si la red falla por un motivo que
 * no es un error propio del backend ([ApioApiException] -- ej. "fuera del
 * area piloto"), se devuelve la ultima ruta guardada para ese mismo destino
 * en vez de romper la pantalla.
 */
class ApioRepository(
    private val api: ApioApi,
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
                // GeoJSON viene como [lon, lat]; LatLon guarda (lat, lon).
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

    /** Pide al backend la recomendacion en lenguaje natural del LLM para la
     * ruta actual. Le pasa solo datos reales que la app ya tiene: la ruta
     * del backend, el trafico de TomTom de las avenidas que cruza y el
     * resultado de la heuristica local [EvaluacionRiesgo]. */
    suspend fun obtenerRecomendacionIa(
        ruta: EstadoRuta,
        trafico: List<AvenidaTrafico>,
        riesgo: EvaluacionRiesgo,
    ): String {
        val datos = construirRecomendacionRequest(ruta, trafico, riesgo)
        return api.obtenerRecomendacionIa(datos).texto
    }

    /** Guarda en el historial local un traslado por cada ruta calculada. */
    @OptIn(ExperimentalTime::class)
    suspend fun registrarTraslado(ruta: EstadoRuta) {
        cache?.registrarTraslado(
            construirTraslado(ruta, fechaMs = Clock.System.now().toEpochMilliseconds()),
        )
    }

    /** Ultimos traslados, del mas nuevo al mas viejo (vacio si no hay cache). */
    suspend fun obtenerHistorial(): List<Traslado> = cache?.obtenerTraslados(LIMITE_HISTORIAL) ?: emptyList()

    /**
     * Establecimientos de salud de Arequipa: primero del backend
     * (/api/establecimientos, RENIPRESS + OSM) y se guardan en el cache
     * local (Room); si no hay red, la ultima lista guardada; y si nunca se
     * descargo, la lista fija [HOSPITALES] para que la app siga usable.
     * Devuelve tambien de donde salieron, para que el ViewModel sepa si
     * debe volver a intentar con el servidor.
     */
    suspend fun obtenerHospitales(): ListaHospitales {
        try {
            val remotos = api.obtenerEstablecimientos().establecimientos.map(::aHospital)
            if (remotos.isNotEmpty()) {
                try {
                    cache?.guardarHospitales(remotos)
                } catch (_: Exception) {
                    // best-effort: la lista ya llego, el cache es secundario
                }
                return ListaHospitales(remotos, OrigenDatos.SERVIDOR)
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            // sin red o backend sin datos: se usa el respaldo local
        }
        val cacheados = try {
            cache?.obtenerHospitalesCache().orEmpty()
        } catch (_: Exception) {
            emptyList()
        }
        return if (cacheados.isNotEmpty()) {
            ListaHospitales(cacheados, OrigenDatos.CACHE)
        } else {
            ListaHospitales(HOSPITALES, OrigenDatos.RESPALDO)
        }
    }

    fun cerrar() = api.cerrar()
}

private const val LIMITE_HISTORIAL = 50

/** DTO del backend -> modelo de la app. */
internal fun aHospital(dto: EstablecimientoDto): Hospital = Hospital(
    nombre = dto.nombre,
    tipo = dto.tipo,
    posicion = LatLon(dto.lat, dto.lon),
    codigo = dto.codigo,
    categoria = dto.categoria,
    emergencia = dto.emergencia,
    distrito = dto.distrito,
    telefono = dto.telefono,
)

/** Convierte una ruta calculada en el registro de historial. El privilegio
 * principal es el de mayor puntaje de la ruta (el que mas ayuda a llegar). */
internal fun construirTraslado(ruta: EstadoRuta, fechaMs: Long): Traslado = Traslado(
    destino = ruta.destino.nombre,
    tipoDestino = ruta.destino.tipo,
    fechaMs = fechaMs,
    distanciaM = ruta.distanciaM,
    tiempoS = ruta.tiempoS,
    tiempoSConTrafico = ruta.tiempoSConTrafico,
    privilegioPrincipal = ruta.privilegiosCruzados
        .maxByOrNull { it.tipoPrincipalScore ?: it.general }
        ?.tipoPrincipal,
)

/** Arma el pedido para /api/ia/recomendacion a partir de datos que la app ya
 * tiene (ruta + trafico + heuristica local). Separado de la clase para poder
 * probarlo en commonTest sin pasar por Ktor ni por el backend real. */
internal fun construirRecomendacionRequest(
    ruta: EstadoRuta,
    trafico: List<AvenidaTrafico>,
    riesgo: EvaluacionRiesgo,
): RecomendacionRequest {
    val nivelPorAvenida = trafico.associate { it.avenida to it.nivel }
    return RecomendacionRequest(
        destino = ruta.destino.nombre,
        distanciaM = ruta.distanciaM,
        tiempoS = ruta.tiempoS,
        tiempoSConTrafico = ruta.tiempoSConTrafico,
        avenidasTrafico = ruta.avenidasTraficoCruzadas.map { avenida ->
            AvenidaNivel(avenida, nivelPorAvenida[avenida] ?: "desconocido")
        },
        privilegios = ruta.privilegiosCruzados.map { p ->
            PrivilegioRuta(p.avenida, p.tipoPrincipal, p.tipoPrincipalScore ?: p.general)
        },
        riesgoNivel = riesgo.nivel.name,
        riesgoPuntaje = riesgo.puntaje,
    )
}

/** De donde salio una lista de datos. */
enum class OrigenDatos { SERVIDOR, CACHE, RESPALDO }

data class ListaHospitales(val hospitales: List<Hospital>, val origen: OrigenDatos)
