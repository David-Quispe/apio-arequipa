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
import pe.apio.mobile.modelo.LatLon
import pe.apio.mobile.ui.EstadoPantalla
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Pruebas de "app sin crashes": cada fallo de red/IA/cache debe terminar en
 * un estado de pantalla valido, nunca en una excepcion que tumbe la app. */
@OptIn(ExperimentalCoroutinesApi::class)
class MapaViewModelTest {

    private val api = ApiProgramable()
    private val cache = CacheFalso()

    @BeforeTest
    fun antes() = Dispatchers.setMain(StandardTestDispatcher())

    @AfterTest
    fun despues() = Dispatchers.resetMain()

    private fun crear(cacheStore: pe.apio.mobile.datos.RutaCacheStore? = cache) =
        MapaViewModel(ApioRepository(api, cacheStore))

    @Test
    fun ruta_exitosa_muestra_exito_y_registra_el_traslado() = runTest {
        val vm = crear()
        runCurrent()
        vm.seleccionarHospital(HOSPITAL_PRUEBA)
        runCurrent()

        val estado = vm.uiState.value.estado
        assertIs<EstadoPantalla.Exito>(estado)
        assertEquals(HOSPITAL_PRUEBA.nombre, estado.ruta.destino.nombre)
        assertEquals(1, cache.traslados.size)
        vm.viewModelScope.cancel()
    }

    @Test
    fun sin_red_y_sin_cache_la_pantalla_queda_en_error_sin_crash() = runTest {
        api.errorRuta = RuntimeException("Sin conexion")
        val vm = crear(cacheStore = null)
        runCurrent()
        vm.seleccionarHospital(HOSPITAL_PRUEBA)
        runCurrent()

        val estado = vm.uiState.value.estado
        assertIs<EstadoPantalla.Error>(estado)
        assertEquals("Sin conexion", estado.mensaje)
        vm.viewModelScope.cancel()
    }

    @Test
    fun sin_red_pero_con_ruta_en_cache_se_muestra_la_cacheada() = runTest {
        val vm = crear()
        runCurrent()
        vm.seleccionarHospital(HOSPITAL_PRUEBA)
        runCurrent()

        api.errorRuta = RuntimeException("Sin conexion")
        vm.seleccionarHospital(HOSPITAL_PRUEBA)
        runCurrent()

        assertIs<EstadoPantalla.Exito>(vm.uiState.value.estado)
        vm.viewModelScope.cancel()
    }

    @Test
    fun si_falla_el_trafico_el_panel_no_se_cae() = runTest {
        api.errorTrafico = RuntimeException("TomTom caido")
        val vm = crear()
        runCurrent()

        assertTrue(vm.uiState.value.trafico.isEmpty())
        assertIs<EstadoPantalla.Vacio>(vm.uiState.value.estado)
        vm.viewModelScope.cancel()
    }

    @Test
    fun si_falla_la_ia_se_oculta_la_tarjeta_y_no_hay_error() = runTest {
        api.errorIa = RuntimeException("Gemini sin cuota")
        val vm = crear()
        runCurrent()
        vm.seleccionarHospital(HOSPITAL_PRUEBA)
        runCurrent()

        assertIs<EstadoPantalla.Exito>(vm.uiState.value.estado)
        assertEquals(EstadoRecomendacion.NoDisponible, vm.uiState.value.recomendacion)
        vm.viewModelScope.cancel()
    }

    @Test
    fun si_funciona_la_ia_se_guarda_el_texto() = runTest {
        val vm = crear()
        runCurrent()
        vm.seleccionarHospital(HOSPITAL_PRUEBA)
        runCurrent()

        assertEquals(EstadoRecomendacion.Lista(api.textoIa), vm.uiState.value.recomendacion)
        vm.viewModelScope.cancel()
    }

    @Test
    fun si_room_falla_la_ruta_se_muestra_igual() = runTest {
        val vm = crear(cacheStore = CacheRoto())
        runCurrent()
        vm.seleccionarHospital(HOSPITAL_PRUEBA)
        runCurrent()

        assertIs<EstadoPantalla.Exito>(vm.uiState.value.estado)
        vm.viewModelScope.cancel()
    }

    @Test
    fun mover_el_origen_borra_la_ruta_activa() = runTest {
        val vm = crear()
        runCurrent()
        vm.seleccionarHospital(HOSPITAL_PRUEBA)
        runCurrent()

        val nuevo = LatLon(-16.39, -71.54)
        vm.moverOrigen(nuevo)

        assertEquals(nuevo, vm.uiState.value.origen)
        assertIs<EstadoPantalla.Vacio>(vm.uiState.value.estado)
        assertNull(vm.uiState.value.riesgo)
        vm.viewModelScope.cancel()
    }

    @Test
    fun cerrar_panel_vuelve_a_vacio() = runTest {
        val vm = crear()
        runCurrent()
        vm.seleccionarHospital(HOSPITAL_PRUEBA)
        runCurrent()

        vm.cerrarPanel()

        assertIs<EstadoPantalla.Vacio>(vm.uiState.value.estado)
        vm.viewModelScope.cancel()
    }

    @Test
    fun al_llegar_al_hospital_con_el_gps_se_cierra_el_traslado() = runTest {
        val vm = crear()
        runCurrent()
        vm.seleccionarHospital(HOSPITAL_PRUEBA)
        runCurrent()

        vm.actualizarUbicacionReal(HOSPITAL_PRUEBA.posicion)

        assertEquals(HOSPITAL_PRUEBA.nombre, vm.uiState.value.llegadaA)
        assertIs<EstadoPantalla.Vacio>(vm.uiState.value.estado)

        vm.confirmarLlegada()
        assertNull(vm.uiState.value.llegadaA)
        vm.viewModelScope.cancel()
    }

    @Test
    fun el_gps_lejos_del_hospital_no_cierra_la_ruta() = runTest {
        val vm = crear()
        runCurrent()
        vm.seleccionarHospital(HOSPITAL_PRUEBA)
        runCurrent()

        vm.actualizarUbicacionReal(LatLon(-16.38, -71.55))
        runCurrent()

        assertNull(vm.uiState.value.llegadaA)
        assertIs<EstadoPantalla.Exito>(vm.uiState.value.estado)
        vm.viewModelScope.cancel()
    }
}
