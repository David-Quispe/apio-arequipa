package pe.apio.mobile.viewmodel

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import pe.apio.mobile.datos.ApioRepository
import pe.apio.mobile.datos.CacheFalso
import pe.apio.mobile.datos.OrigenDatos
import pe.apio.mobile.modelo.HOSPITALES
import pe.apio.mobile.modelo.Traslado
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Casos de "sin crashes" para el historial y para la lista de hospitales
 * cuando Room falla (los casos de red de Hospitales estan en
 * HospitalesViewModelTest). */
@OptIn(ExperimentalCoroutinesApi::class)
class HistorialViewModelTest {

    private val api = ApiProgramable()
    private val cache = CacheFalso()

    @BeforeTest
    fun antes() = Dispatchers.setMain(StandardTestDispatcher())

    @AfterTest
    fun despues() = Dispatchers.resetMain()

    @Test
    fun historial_muestra_los_traslados_guardados() = runTest {
        cache.traslados += Traslado("Hospital de Prueba", "MINSA", 1_000L, 5000.0, 600.0, 700.0, null)
        val vm = HistorialViewModel(ApioRepository(api, cache))
        runCurrent()

        assertEquals(1, vm.uiState.value.traslados.size)
        vm.viewModelScope.cancel()
    }

    @Test
    fun historial_con_room_roto_queda_vacio_y_no_se_cae() = runTest {
        val vm = HistorialViewModel(ApioRepository(api, CacheRoto()))
        runCurrent()

        assertTrue(vm.uiState.value.traslados.isEmpty())
        vm.viewModelScope.cancel()
    }

    @Test
    fun hospitales_sin_red_y_con_room_roto_usan_el_respaldo() = runTest {
        api.errorEstablecimientos = RuntimeException("Sin conexion")
        val vm = HospitalesViewModel(ApioRepository(api, CacheRoto()))
        runCurrent()

        assertEquals(OrigenDatos.RESPALDO, vm.uiState.value.origen)
        assertEquals(HOSPITALES, vm.uiState.value.hospitales)
        vm.viewModelScope.cancel()
    }
}
