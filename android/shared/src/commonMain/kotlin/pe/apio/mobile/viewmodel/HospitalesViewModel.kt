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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import pe.apio.mobile.datos.ApioRepository
import pe.apio.mobile.datos.OrigenDatos
import pe.apio.mobile.modelo.HOSPITALES
import pe.apio.mobile.modelo.Hospital

// Cada cuanto se reintenta bajar la lista del servidor mientras la app esta
// usando el cache o la lista de respaldo (por ejemplo, si abrio sin red).
internal const val REINTENTO_HOSPITALES_MS = 30_000L

data class HospitalesUiState(
    val hospitales: List<Hospital> = HOSPITALES,
    val origen: OrigenDatos = OrigenDatos.RESPALDO,
    val cargando: Boolean = true,
)

/**
 * ViewModel de los establecimientos de salud (los usan la pantalla
 * Hospitales y los marcadores del mapa). Si no pudo bajar la lista del
 * servidor, sigue intentando cada [REINTENTO_HOSPITALES_MS] hasta lograrlo,
 * asi la app pasa sola de los 4 hospitales de respaldo a los ~300 reales
 * cuando vuelve la red.
 */
class HospitalesViewModel(private val repositorio: ApioRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(HospitalesUiState())
    val uiState: StateFlow<HospitalesUiState> = _uiState.asStateFlow()

    private var cargaJob: Job? = null

    init {
        viewModelScope.launch {
            cargar()
            while (isActive && _uiState.value.origen != OrigenDatos.SERVIDOR) {
                delay(REINTENTO_HOSPITALES_MS)
                cargar()
            }
        }
    }

    /** Vuelve a pedir la lista ya (boton Reintentar). No hace nada si ya
     * vino del servidor o si hay una carga en curso. */
    fun recargarSiHaceFalta() {
        if (_uiState.value.origen == OrigenDatos.SERVIDOR || cargaJob?.isActive == true) return
        cargaJob = viewModelScope.launch { cargar() }
    }

    private suspend fun cargar() {
        _uiState.update { it.copy(cargando = true) }
        try {
            val resultado = repositorio.obtenerHospitales()
            _uiState.update {
                // no cambiar una lista del servidor por una peor
                if (it.origen == OrigenDatos.SERVIDOR && resultado.origen != OrigenDatos.SERVIDOR) {
                    it.copy(cargando = false)
                } else {
                    it.copy(hospitales = resultado.hospitales, origen = resultado.origen, cargando = false)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            _uiState.update { it.copy(cargando = false) }
        }
    }
}
