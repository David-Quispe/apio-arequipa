package pe.apio.mobile

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.StateFlow
import pe.apio.mobile.datos.ApioRepository
import pe.apio.mobile.modelo.LatLon
import pe.apio.mobile.red.ApioApiClient
import pe.apio.mobile.ui.IconosApio
import pe.apio.mobile.ui.pantallas.PantallaHistorial
import pe.apio.mobile.ui.pantallas.PantallaHospitales
import pe.apio.mobile.ui.pantallas.PantallaMapa
import pe.apio.mobile.ui.pantallas.PantallaPerfil
import pe.apio.mobile.ui.theme.ApioTheme
import pe.apio.mobile.viewmodel.HistorialViewModel
import pe.apio.mobile.viewmodel.HospitalesViewModel
import pe.apio.mobile.viewmodel.MapaViewModel
import pe.apio.mobile.viewmodel.RepositorioHolder

// Backend desplegado en la VM de Azure (NGINX + HTTPS de Let's Encrypt,
// dominio DuckDNS): funciona desde cualquier celular con datos o Wi-Fi.
private const val API_BASE_URL = "https://apio-arequipa.duckdns.org/api"

// Para desarrollo con el backend en tu PC y el emulador, cambia la de arriba
// por esta (10.0.2.2 es el alias del emulador hacia el localhost del host;
// ver network_security_config.xml para el permiso de trafico sin cifrar):
// private const val API_BASE_URL = "http://10.0.2.2:8000/api"

/** Pestanas de la barra de navegacion inferior del diseno de Figma. */
private enum class Pestana(val titulo: String) {
    MAPA("Mapa"),
    HOSPITALES("Hospitales"),
    HISTORIAL("Historial"),
    PERFIL("Perfil"),
}

private fun Pestana.icono(): ImageVector = when (this) {
    Pestana.MAPA -> IconosApio.Mapa
    Pestana.HOSPITALES -> IconosApio.Hospital
    Pestana.HISTORIAL -> IconosApio.Historial
    Pestana.PERFIL -> IconosApio.Persona
}

/**
 * La V del MVVM: dibuja el estado de [MapaViewModel], [HospitalesViewModel]
 * e [HistorialViewModel] y les avisa lo que hace el usuario. Los ViewModels
 * viven mientras la Activity, asi que cambiar de pestana no recalcula nada. El
 * unico estado propio de la vista es de interfaz pura (pestana elegida y
 * tema oscuro), que no es logica de la app.
 *
 * [crearRepositorio] lo inyecta MainActivity (con Retrofit y Room ya
 * conectados); por defecto crea uno sin cache, solo para el @Preview.
 * [ubicacionReal] es la ubicacion GPS que entrega SeguimientoUbicacionServicio
 * cuando esta prendido el seguimiento real; null mientras esta apagado.
 */
@Composable
@Preview
fun App(
    crearRepositorio: () -> ApioRepository = { ApioRepository(ApioApiClient(API_BASE_URL)) },
    ubicacionReal: StateFlow<LatLon?>? = null,
    ubicacionActiva: Boolean = false,
    onToggleUbicacion: () -> Unit = {},
) {
    // Un repositorio compartido y un ViewModel por responsabilidad (MVVM):
    // Mapa (ruta, trafico, IA, GPS), Hospitales (lista) e Historial (Room).
    val repositorio = viewModel { RepositorioHolder(crearRepositorio()) }.repositorio
    val viewModel = viewModel { MapaViewModel(repositorio) }
    val hospitalesViewModel = viewModel { HospitalesViewModel(repositorio) }
    val historialViewModel = viewModel { HistorialViewModel(repositorio) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val hospitalesState by hospitalesViewModel.uiState.collectAsStateWithLifecycle()
    val historialState by historialViewModel.uiState.collectAsStateWithLifecycle()

    var pestana by rememberSaveable { mutableStateOf(Pestana.MAPA) }
    val temaSistemaOscuro = isSystemInDarkTheme()
    // null = seguir al sistema; el switch de Perfil lo fija en true/false
    var temaOscuroElegido by rememberSaveable { mutableStateOf<Boolean?>(null) }
    val temaOscuro = temaOscuroElegido ?: temaSistemaOscuro

    // al entrar a Historial o Perfil se leen los ultimos traslados de Room
    LaunchedEffect(pestana) {
        if (pestana == Pestana.HISTORIAL || pestana == Pestana.PERFIL) historialViewModel.recargar()
    }

    if (ubicacionReal != null) {
        LaunchedEffect(ubicacionReal) {
            ubicacionReal.collect { ubicacion -> ubicacion?.let(viewModel::actualizarUbicacionReal) }
        }
    }

    ApioTheme(oscuro = temaOscuro) {
        Scaffold(
            // Sin insets automaticos: el mapa va de borde a borde (debajo de
            // la barra de estado) y cada pantalla maneja su propio margen.
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                NavigationBar {
                    Pestana.entries.forEach { opcion ->
                        NavigationBarItem(
                            selected = pestana == opcion,
                            onClick = { pestana = opcion },
                            icon = { Icon(opcion.icono(), contentDescription = null) },
                            label = { Text(opcion.titulo) },
                        )
                    }
                }
            },
        ) { relleno ->
            val contenido = Modifier.fillMaxSize().padding(bottom = relleno.calculateBottomPadding())
            when (pestana) {
                Pestana.MAPA -> PantallaMapa(
                    uiState = uiState,
                    hospitales = hospitalesState.hospitales,
                    seguimientoActivo = ubicacionActiva,
                    onToggleSeguimiento = onToggleUbicacion,
                    onTapMapa = viewModel::moverOrigen,
                    onTapHospital = viewModel::seleccionarHospital,
                    onCerrarRuta = viewModel::cerrarPanel,
                    onReintentar = { hospital ->
                        // si la lista de hospitales no vino del servidor, se pide de nuevo
                        hospitalesViewModel.recargarSiHaceFalta()
                        viewModel.seleccionarHospital(hospital)
                    },
                    onBuscar = { pestana = Pestana.HOSPITALES },
                    modifier = contenido,
                )
                Pestana.HOSPITALES -> PantallaHospitales(
                    hospitales = hospitalesState.hospitales,
                    origen = uiState.origen,
                    estado = uiState.estado,
                    onElegir = { hospital ->
                        viewModel.seleccionarHospital(hospital)
                        pestana = Pestana.MAPA
                    },
                    modifier = contenido.statusBarsPadding(),
                )
                Pestana.HISTORIAL -> PantallaHistorial(
                    historial = historialState.traslados,
                    onNuevaEmergencia = { pestana = Pestana.HOSPITALES },
                    modifier = contenido.statusBarsPadding(),
                )
                Pestana.PERFIL -> PantallaPerfil(
                    historial = historialState.traslados,
                    temaOscuro = temaOscuro,
                    onCambiarTemaOscuro = { temaOscuroElegido = it },
                    seguimientoActivo = ubicacionActiva,
                    onToggleSeguimiento = onToggleUbicacion,
                    modifier = contenido.statusBarsPadding(),
                )
            }
        }
    }
}
