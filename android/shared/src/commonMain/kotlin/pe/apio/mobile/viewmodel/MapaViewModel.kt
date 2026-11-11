package pe.apio.mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.apio.mobile.datos.ApioRepository
import pe.apio.mobile.ia.EvaluacionRiesgo
import pe.apio.mobile.ia.evaluarRiesgo
import pe.apio.mobile.modelo.AvenidaTrafico
import pe.apio.mobile.modelo.HOSPITALES
import pe.apio.mobile.modelo.Hospital
import pe.apio.mobile.modelo.LatLon
import pe.apio.mobile.modelo.Traslado
import pe.apio.mobile.ui.EstadoPantalla
import pe.apio.mobile.ui.EstadoRuta

// Cerro Colorado, zona de origen del corredor (mismo punto que
// ORIGEN_INICIAL en frontend/src/App.jsx).
private val ORIGEN_INICIAL = LatLon(-16.3833, -71.55)

// Mismo intervalo que TRAFICO_INTERVALO_MS en frontend/src/App.jsx.
private const val TRAFICO_INTERVALO_MS = 25_000L

/**
 * Todo lo que la pantalla necesita para dibujarse, en un solo objeto
 * inmutable. La UI solo lee esto: nunca modifica el estado directamente.
 */
data class MapaUiState(
    val origen: LatLon = ORIGEN_INICIAL,
    val hospitales: List<Hospital> = HOSPITALES,
    val estado: EstadoPantalla = EstadoPantalla.Vacio,
    val trafico: List<AvenidaTrafico> = emptyList(),
    val riesgo: EvaluacionRiesgo? = null,
    val recomendacion: EstadoRecomendacion? = null,
    val historial: List<Traslado> = emptyList(),
)

/** Estado de la recomendacion del LLM (Gemini, via backend). Si no esta
 * disponible, el panel se queda solo con el mensaje de la heuristica local. */
sealed interface EstadoRecomendacion {
    data object Cargando : EstadoRecomendacion
    data class Lista(val texto: String) : EstadoRecomendacion
    data object NoDisponible : EstadoRecomendacion
}

/** Recalcula el riesgo IA (ver pe.apio.mobile.ia.EstimadorRiesgo) a partir
 * del estado actual: solo tiene sentido con una ruta ya calculada, y debe
 * reaccionar tanto a una ruta nueva como a un refresco de trafico. */
private fun MapaUiState.conRiesgoRecalculado(): MapaUiState {
    val exito = estado as? EstadoPantalla.Exito ?: return copy(riesgo = null)
    return copy(riesgo = evaluarRiesgo(exito.ruta, trafico))
}

/**
 * ViewModel de la pantalla del mapa (la V del MVVM es [pe.apio.mobile.App] y
 * los paneles; la M son los modelos y el [ApioRepository]).
 *
 * Flujo unidireccional: la UI llama a las funciones publicas (eventos) y
 * observa [uiState]. Sobrevive a rotaciones, asi que ya no se pierde la ruta
 * ni se reinicia el sondeo cuando la Activity se recrea.
 */
class MapaViewModel(private val repositorio: ApioRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(MapaUiState())
    val uiState: StateFlow<MapaUiState> = _uiState.asStateFlow()

    // Pedido de ruta en curso. Se cancela si el usuario toca otro hospital o
    // mueve el origen, para que una respuesta vieja no pise la nueva.
    private var rutaJob: Job? = null

    // Pedido al LLM en curso y la situacion (destino + nivel de riesgo) para
    // la que se pidio. Solo se vuelve a llamar a la IA cuando esa situacion
    // cambia, no en cada sondeo de 25 s (cuida la cuota del free tier).
    private var iaJob: Job? = null
    private var claveRecomendacion: String? = null

    init {
        viewModelScope.launch {
            // RENIPRESS + OSM desde el backend, con respaldo en cache y en la
            // lista local HOSPITALES (que ya esta en uiState mientras carga)
            val hospitales = repositorio.obtenerHospitales()
            _uiState.update { it.copy(hospitales = hospitales) }
        }
        viewModelScope.launch { recargarHistorial() }
        iniciarSondeo()
    }

    // ---- Eventos que llegan desde la UI ----

    fun seleccionarHospital(hospital: Hospital) {
        rutaJob?.cancel()
        reiniciarRecomendacion()
        _uiState.update { it.copy(estado = EstadoPantalla.Cargando(hospital), recomendacion = null) }
        val origen = _uiState.value.origen
        rutaJob = viewModelScope.launch {
            try {
                val ruta = repositorio.obtenerRuta(origen, hospital)
                _uiState.update { it.copy(estado = EstadoPantalla.Exito(ruta)).conRiesgoRecalculado() }
                revisarRecomendacion()
                registrarEnHistorial(ruta)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        estado = EstadoPantalla.Error(
                            e.message ?: "No se pudo calcular la ruta",
                            hospital,
                        ),
                    )
                }
            }
        }
    }

    fun moverOrigen(nuevoOrigen: LatLon) {
        rutaJob?.cancel()
        reiniciarRecomendacion()
        _uiState.update {
            it.copy(origen = nuevoOrigen, estado = EstadoPantalla.Vacio, riesgo = null, recomendacion = null)
        }
    }

    fun cerrarPanel() {
        rutaJob?.cancel()
        reiniciarRecomendacion()
        _uiState.update { it.copy(estado = EstadoPantalla.Vacio, riesgo = null, recomendacion = null) }
    }

    // ---- Historial de traslados ----

    /** Cada ruta elegida por el usuario queda como un traslado en el
     * historial local (no los refrescos automaticos de 25 s). */
    private fun registrarEnHistorial(ruta: EstadoRuta) {
        viewModelScope.launch {
            try {
                repositorio.registrarTraslado(ruta)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // best-effort: un fallo del historial no afecta la ruta
            }
            recargarHistorial()
        }
    }

    private suspend fun recargarHistorial() {
        try {
            val historial = repositorio.obtenerHistorial()
            _uiState.update { it.copy(historial = historial) }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // sin cache disponible: el historial queda vacio
        }
    }

    // ---- Recomendacion del LLM ----

    private fun reiniciarRecomendacion() {
        iaJob?.cancel()
        claveRecomendacion = null
    }

    /** Pide una recomendacion nueva al LLM solo si hay ruta + riesgo y la
     * situacion cambio desde la ultima vez (otro destino u otro nivel). */
    private fun revisarRecomendacion() {
        val actual = _uiState.value
        val ruta = (actual.estado as? EstadoPantalla.Exito)?.ruta ?: return
        val riesgo = actual.riesgo ?: return
        val clave = "${ruta.destino.nombre}|${riesgo.nivel}"
        if (clave == claveRecomendacion) return
        claveRecomendacion = clave

        iaJob?.cancel()
        _uiState.update { it.copy(recomendacion = EstadoRecomendacion.Cargando) }
        iaJob = viewModelScope.launch {
            val nuevo = try {
                EstadoRecomendacion.Lista(repositorio.obtenerRecomendacionIa(ruta, actual.trafico, riesgo))
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // sin key, sin red o cuota agotada: la heuristica local sigue
                // en pantalla, asi que no se muestra ningun error
                EstadoRecomendacion.NoDisponible
            }
            _uiState.update { it.copy(recomendacion = nuevo) }
        }
    }

    // ---- Sondeo de trafico + auto-refresh del ETA ----

    private fun iniciarSondeo() {
        viewModelScope.launch {
            while (true) {
                try {
                    val trafico = repositorio.obtenerTrafico()
                    _uiState.update { it.copy(trafico = trafico).conRiesgoRecalculado() }
                    revisarRecomendacion()
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // silencioso: un fallo en el sondeo no debe tirar abajo el
                    // panel que ya se esta mostrando
                }

                refrescarRutaActiva()
                delay(TRAFICO_INTERVALO_MS)
            }
        }
    }

    /** Si hay una ruta activa, la vuelve a pedir en silencio (sin pasar por
     * Cargando) para que el ETA se actualice solo con el trafico mas
     * reciente. El camino (`puntos`) no cambia: el trafico ajusta el ETA, no
     * la ruta. */
    private suspend fun refrescarRutaActiva() {
        val inicial = _uiState.value
        val exito = inicial.estado as? EstadoPantalla.Exito ?: return
        val destino = exito.ruta.destino

        try {
            val nueva = repositorio.obtenerRuta(inicial.origen, destino)
            _uiState.update { actual ->
                val estadoActual = actual.estado
                // Solo se aplica si el usuario sigue viendo la misma ruta.
                if (estadoActual is EstadoPantalla.Exito &&
                    estadoActual.ruta.destino.nombre == destino.nombre &&
                    actual.origen == inicial.origen
                ) {
                    actual.copy(
                        estado = EstadoPantalla.Exito(
                            estadoActual.ruta.copy(
                                tiempoS = nueva.tiempoS,
                                tiempoSConTrafico = nueva.tiempoSConTrafico,
                                privilegiosCruzados = nueva.privilegiosCruzados,
                                avenidasTraficoCruzadas = nueva.avenidasTraficoCruzadas,
                            ),
                        ),
                    ).conRiesgoRecalculado()
                } else {
                    actual
                }
            }
            revisarRecomendacion()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // silencioso: no pisar una ruta ya mostrada por un fallo de esta
            // actualizacion periodica
        }
    }

    override fun onCleared() {
        repositorio.cerrar()
    }
}
