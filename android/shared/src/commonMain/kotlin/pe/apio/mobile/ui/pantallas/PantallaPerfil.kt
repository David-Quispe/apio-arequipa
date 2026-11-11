package pe.apio.mobile.ui.pantallas

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pe.apio.mobile.modelo.Traslado
import pe.apio.mobile.ui.IconosApio
import pe.apio.mobile.ui.formatearKmCorto
import pe.apio.mobile.ui.minutosCortos

/**
 * Pantalla "04 · Perfil" del diseno de Figma. No hay login en APIO, asi que
 * en vez de un conductor inventado muestra la unidad y estadisticas reales
 * del historial local, y ajustes que si funcionan: tema oscuro y el
 * seguimiento GPS real (foreground service).
 */
@Composable
fun PantallaPerfil(
    historial: List<Traslado>,
    temaOscuro: Boolean,
    onCambiarTemaOscuro: (Boolean) -> Unit,
    seguimientoActivo: Boolean,
    onToggleSeguimiento: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text("Perfil", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(20.dp))

        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(96.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("AMB", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimary)
            }
            Spacer(Modifier.height(12.dp))
            Text("Unidad de ambulancia", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Corredor piloto · Cono Norte → hospitales del Cercado",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (seguimientoActivo) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (seguimientoActivo) IconosApio.Check else IconosApio.MiUbicacion,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (seguimientoActivo) "En servicio · GPS activo" else "GPS real apagado",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        val promedioS = if (historial.isEmpty()) 0.0 else historial.map { it.tiempoSConTrafico }.average()
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TarjetaDato(IconosApio.Ruta, "${historial.size}", "traslados", Modifier.weight(1f))
            TarjetaDato(
                IconosApio.Historial,
                if (historial.isEmpty()) "–" else "${minutosCortos(promedioS)} min",
                "promedio",
                Modifier.weight(1f),
            )
            TarjetaDato(IconosApio.Navegacion, formatearKmCorto(historial.sumOf { it.distanciaM }), "recorridos", Modifier.weight(1f))
        }

        Spacer(Modifier.height(24.dp))
        Text("Preferencias", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                FilaAjuste(IconosApio.ModoOscuro, "Tema oscuro", "Colores oscuros para turnos de noche") {
                    Switch(checked = temaOscuro, onCheckedChange = onCambiarTemaOscuro)
                }
                HorizontalDivider(Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant)
                FilaAjuste(
                    IconosApio.MiUbicacion,
                    "Seguimiento GPS real",
                    "Servicio en primer plano con la ubicación del dispositivo",
                ) {
                    Switch(checked = seguimientoActivo, onCheckedChange = { onToggleSeguimiento() })
                }
                HorizontalDivider(Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant)
                FilaAjuste(IconosApio.Ia, "Recomendaciones con IA", "Google Gemini a través del backend de APIO") {}
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "APIO · versión 1.0 · TECSUP 2026",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun TarjetaDato(icono: ImageVector, valor: String, etiqueta: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier,
    ) {
        Column(Modifier.padding(14.dp)) {
            Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(8.dp))
            Text(valor, style = MaterialTheme.typography.titleMedium)
            Text(
                etiqueta,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FilaAjuste(
    icono: ImageVector,
    titulo: String,
    descripcion: String,
    accion: @Composable () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.bodyLarge)
            Text(
                descripcion,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        accion()
    }
}
