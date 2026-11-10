package pe.apio.mobile.datos

import pe.apio.mobile.ia.EvaluacionRiesgo
import pe.apio.mobile.modelo.AvenidaNivel
import pe.apio.mobile.modelo.AvenidaTrafico
import pe.apio.mobile.modelo.Hospital
import pe.apio.mobile.modelo.LatLon
import pe.apio.mobile.modelo.PrivilegioRuta
import pe.apio.mobile.modelo.RecomendacionRequest
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

    /** Escribe el listado de hospitales en el cache local. Hoy [Hospital]
     * sale de la constante hardcodeada HOSPITALES (no hay endpoint de red
     * todavia), pero dejar esto conectado hace trivial el dia que haya uno. */
    suspend fun sincronizarHospitalesCache(hospitales: List<Hospital>) {
        cache?.guardarHospitales(hospitales)
    }

    fun cerrar() = api.cerrar()
}

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
