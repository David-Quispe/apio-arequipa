package pe.apio.mobile.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import pe.apio.mobile.datos.ApiFalsa
import pe.apio.mobile.datos.ApioRepository
import pe.apio.mobile.datos.CacheFalso
import pe.apio.mobile.datos.OrigenDatos
import pe.apio.mobile.modelo.EstablecimientoDto
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class HospitalesViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val api = ApiFalsa()
    private val repositorio = ApioRepository(api, CacheFalso())

    private val hospitalesReales = (1..5).map {
        EstablecimientoDto(nombre = "Establecimiento $it", emergencia = true, lat = -16.4, lon = -71.5)
    }

    @BeforeTest
    fun preparar() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun limpiar() = Dispatchers.resetMain()

    @Test
    fun conRedCargaLaListaDelServidor() = runTest(dispatcher) {
        api.establecimientos = hospitalesReales

        val vm = HospitalesViewModel(repositorio)
        runCurrent()

        assertEquals(OrigenDatos.SERVIDOR, vm.uiState.value.origen)
        assertEquals(5, vm.uiState.value.hospitales.size)
        assertEquals(false, vm.uiState.value.cargando)
    }

    @Test
    fun sinRedReintentaSoloHastaQueVuelveLaRed() = runTest(dispatcher) {
        api.errorRed = RuntimeException("sin internet")
        val vm = HospitalesViewModel(repositorio)
        runCurrent()
        assertEquals(OrigenDatos.RESPALDO, vm.uiState.value.origen)

        // vuelve la red: en el siguiente reintento automatico llega la lista real
        api.errorRed = null
        api.establecimientos = hospitalesReales
        avanzar(REINTENTO_HOSPITALES_MS)

        assertEquals(OrigenDatos.SERVIDOR, vm.uiState.value.origen)
        assertEquals(5, vm.uiState.value.hospitales.size)

        // con la lista del servidor ya no sigue pidiendo
        val llamadas = api.llamadasEstablecimientos
        avanzar(REINTENTO_HOSPITALES_MS * 3)
        assertEquals(llamadas, api.llamadasEstablecimientos)
    }

    @Test
    fun reintentarManualPideLaListaDeInmediato() = runTest(dispatcher) {
        api.errorRed = RuntimeException("sin internet")
        val vm = HospitalesViewModel(repositorio)
        runCurrent()

        api.errorRed = null
        api.establecimientos = hospitalesReales
        vm.recargarSiHaceFalta()
        runCurrent()

        assertEquals(OrigenDatos.SERVIDOR, vm.uiState.value.origen)
    }

    private fun TestScope.avanzar(ms: Long) {
        advanceTimeBy(ms)
        runCurrent()
    }
}
