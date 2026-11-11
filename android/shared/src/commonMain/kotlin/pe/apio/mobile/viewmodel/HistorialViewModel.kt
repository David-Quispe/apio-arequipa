package pe.apio.mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.apio.mobile.datos.ApioRepository
import pe.apio.mobile.modelo.Traslado

data class HistorialUiState(
    val traslados: List<Traslado> = emptyList(),
)

/**
 * ViewModel del historial de traslados (pantallas Historial y Perfil). Lee
 * la tabla "traslados" de Room a traves del repositorio; el MapaViewModel
 * es quien registra un traslado nuevo cada vez que se elige un hospital.
 */
class HistorialViewModel(private val repositorio: ApioRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(HistorialUiState())
    val uiState: StateFlow<HistorialUiState> = _uiState.asStateFlow()

    init {
        recargar()
    }

    /** Se llama al entrar a Historial o Perfil, para ver los ultimos viajes. */
    fun recargar() {
        viewModelScope.launch {
            try {
                val traslados = repositorio.obtenerHistorial()
                _uiState.update { it.copy(traslados = traslados) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // sin cache disponible: el historial queda como estaba
            }
        }
    }
}
