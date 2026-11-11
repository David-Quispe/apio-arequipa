package pe.apio.mobile.ui.pantallas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pe.apio.mobile.mapa.MapaOSM
import pe.apio.mobile.modelo.Hospital
import pe.apio.mobile.modelo.LatLon
import pe.apio.mobile.ui.EstadoPantalla
import pe.apio.mobile.ui.HojaRuta
import pe.apio.mobile.ui.IconosApio
import pe.apio.mobile.viewmodel.MapaUiState

/**
 * Pantalla "01 · Mapa — Ruta sugerida" del diseno de Figma: mapa a pantalla
 * completa, buscador arriba, FAB de ubicacion y la hoja de ruta abajo. Recibe
 * el estado ya listo del ViewModel y solo reenvia eventos.
 */
@Composable
fun PantallaMapa(
    uiState: MapaUiState,
    hospitales: List<Hospital>,
    seguimientoActivo: Boolean,
    onToggleSeguimiento: () -> Unit,
    onTapMapa: (LatLon) -> Unit,
    onTapHospital: (Hospital) -> Unit,
    onCerrarRuta: () -> Unit,
    onReintentar: (Hospital) -> Unit,
    onBuscar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ruta = (uiState.estado as? EstadoPantalla.Exito)?.ruta

    // Por defecto el mapa muestra solo los destinos con emergencia; "Todos"
    // suma los puestos y centros de salud de primer nivel (en gris).
    var verTodos by rememberSaveable { mutableStateOf(false) }
    val totalEmergencia = remember(hospitales) { hospitales.count { it.emergencia } }
    val hospitalesMapa = remember(hospitales, verTodos) {
        if (verTodos) hospitales else hospitales.filter { it.emergencia }
    }

    Box(modifier.fillMaxSize()) {
        MapaOSM(
            modifier = Modifier.fillMaxSize(),
            origen = uiState.origen,
            hospitales = hospitalesMapa,
            rutaPuntos = ruta?.puntos ?: emptyList(),
            onTapMapa = onTapMapa,
            onTapHospital = onTapHospital,
        )

        Column(
            Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth(),
        ) {
            BarraBusqueda(onClick = onBuscar, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChipCapa(
                    texto = "Con emergencia ($totalEmergencia)",
                    seleccionado = !verTodos,
                    onClick = { verTodos = false },
                )
                ChipCapa(
                    texto = "Todos (${hospitales.size})",
                    seleccionado = verTodos,
                    onClick = { verTodos = true },
                )
            }
        }

        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
            FloatingActionButton(
                onClick = onToggleSeguimiento,
                modifier = Modifier.align(Alignment.End).padding(end = 16.dp, bottom = 12.dp),
                containerColor = if (seguimientoActivo) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                contentColor = if (seguimientoActivo) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.primary
                },
            ) {
                Icon(
                    IconosApio.MiUbicacion,
                    contentDescription = if (seguimientoActivo) "Apagar GPS real" else "Usar GPS real",
                )
            }
            HojaRuta(
                estado = uiState.estado,
                riesgo = uiState.riesgo,
                recomendacion = uiState.recomendacion,
                trafico = uiState.trafico,
                seguimientoActivo = seguimientoActivo,
                onToggleSeguimiento = onToggleSeguimiento,
                onCerrar = onCerrarRuta,
                onReintentar = onReintentar,
                onVerHospitales = onBuscar,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChipCapa(texto: String, seleccionado: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = seleccionado,
        onClick = onClick,
        label = { Text(texto) },
        leadingIcon = if (seleccionado) {
            { Icon(IconosApio.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
        } else {
            null
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        elevation = FilterChipDefaults.filterChipElevation(elevation = 3.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BarraBusqueda(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 4.dp,
    ) {
        Row(
            Modifier.height(56.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                IconosApio.Hospital,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                "¿A qué hospital vamos?",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Icon(
                IconosApio.Buscar,
                contentDescription = "Buscar hospital",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
