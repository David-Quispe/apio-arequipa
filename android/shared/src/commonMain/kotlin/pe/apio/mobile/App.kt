package pe.apio.mobile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.StateFlow
import pe.apio.mobile.datos.ApioRepository
import pe.apio.mobile.mapa.MapaOSM
import pe.apio.mobile.modelo.LatLon
import pe.apio.mobile.red.ApioApiClient
import pe.apio.mobile.ui.EstadoPantalla
import pe.apio.mobile.ui.PanelInfo
import pe.apio.mobile.ui.PanelTrafico
import pe.apio.mobile.ui.theme.ApioTheme
import pe.apio.mobile.viewmodel.MapaViewModel

// 10.0.2.2 es el alias del emulador Android hacia el localhost de la
// maquina host, donde corre el backend real (ver AndroidManifest.xml /
// network_security_config.xml para el permiso de trafico sin cifrar).
private const val API_BASE_URL = "http://10.0.2.2:8000/api"

/**
 * La V del MVVM: solo dibuja [MapaViewModel.uiState] y le avisa al
 * ViewModel lo que hace el usuario. No hace red, no guarda estado propio ni
 * lanza corrutinas propias (la unica excepcion es el [LaunchedEffect] que
 * reenvia [ubicacionReal] al ViewModel, que sigue siendo solo "avisar").
 *
 * [crearRepositorio] lo puede inyectar MainActivity (con el cache SQLite ya
 * conectado); por defecto crea uno sin cache, solo para que el @Preview
 * funcione sin Android real. [ubicacionReal] es la ubicacion GPS real que
 * entrega SeguimientoUbicacionServicio cuando esta prendido el "modo
 * ambulancia real" (ver MainActivity); null mientras ese modo esta apagado.
 */
@Composable
@Preview
fun App(
    crearRepositorio: () -> ApioRepository = { ApioRepository(ApioApiClient(API_BASE_URL)) },
    ubicacionReal: StateFlow<LatLon?>? = null,
    ubicacionActiva: Boolean = false,
    onToggleUbicacion: () -> Unit = {},
) {
    val viewModel = viewModel { MapaViewModel(crearRepositorio()) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val ruta = (uiState.estado as? EstadoPantalla.Exito)?.ruta

    if (ubicacionReal != null) {
        LaunchedEffect(ubicacionReal) {
            ubicacionReal.collect { ubicacion -> ubicacion?.let(viewModel::moverOrigen) }
        }
    }

    ApioTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            MapaOSM(
                modifier = Modifier.fillMaxSize(),
                origen = uiState.origen,
                hospitales = uiState.hospitales,
                rutaPuntos = ruta?.puntos ?: emptyList(),
                onTapMapa = viewModel::moverOrigen,
                onTapHospital = viewModel::seleccionarHospital,
            )

            PanelInfo(
                estado = uiState.estado,
                riesgo = uiState.riesgo,
                recomendacion = uiState.recomendacion,
                onDismiss = viewModel::cerrarPanel,
                onRetry = viewModel::seleccionarHospital,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(12.dp)
                    .fillMaxWidth(),
            )

            PanelTrafico(
                avenidas = uiState.trafico,
                avenidasRuta = ruta?.avenidasTraficoCruzadas,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
            )

            BotonUbicacionReal(
                activo = ubicacionActiva,
                onClick = onToggleUbicacion,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            )
        }
    }
}

/** Prende/apaga el foreground service de GPS real (SeguimientoUbicacionServicio
 * en androidApp): cuando esta activo, el origen de la ambulancia deja de
 * moverse solo con el tap manual y sigue la ubicacion real del dispositivo. */
@Composable
private fun BotonUbicacionReal(
    activo: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        containerColor = if (activo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        contentColor = if (activo) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
    ) {
        Text(if (activo) "GPS real: ON" else "GPS real")
    }
}
