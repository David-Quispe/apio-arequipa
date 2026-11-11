package pe.apio.mobile.ui.pantallas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pe.apio.mobile.modelo.Traslado
import pe.apio.mobile.ui.IconosApio
import pe.apio.mobile.ui.diaLocal
import pe.apio.mobile.ui.etiquetaDia
import pe.apio.mobile.ui.formatearDecimal
import pe.apio.mobile.ui.formatearKmCorto
import pe.apio.mobile.ui.horaLocal
import pe.apio.mobile.ui.minutosCortos
import pe.apio.mobile.ui.theme.ApioRojoAlerta
import pe.apio.mobile.ui.theme.ApioVerde
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Pantalla "03 · Historial" del diseno de Figma. Los traslados salen del
 * historial persistido en SQLite (tabla "traslados"): se guarda uno cada vez
 * que se calcula una ruta a un hospital.
 */
@OptIn(ExperimentalTime::class)
@Composable
fun PantallaHistorial(
    historial: List<Traslado>,
    onNuevaEmergencia: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hoy = diaLocal(Clock.System.now().toEpochMilliseconds())
    val grupos = historial.groupBy { diaLocal(it.fechaMs) }

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { Text("Historial", style = MaterialTheme.typography.headlineMedium) }
            item {
                Spacer(Modifier.height(4.dp))
                TarjetaResumen(historial)
                Spacer(Modifier.height(8.dp))
            }
            if (historial.isEmpty()) {
                item { EstadoVacio() }
            } else {
                grupos.forEach { (dia, traslados) ->
                    item {
                        Text(
                            etiquetaDia(dia, hoy),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    items(traslados) { traslado -> FilaTraslado(traslado) }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = onNuevaEmergencia,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            icon = { Icon(IconosApio.Emergencia, contentDescription = null) },
            text = { Text("Nueva emergencia") },
        )
    }
}

@Composable
private fun TarjetaResumen(historial: List<Traslado>) {
    val promedioS = if (historial.isEmpty()) 0.0 else historial.map { it.tiempoSConTrafico }.average()
    val kmTotales = historial.sumOf { it.distanciaM }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                "Resumen de traslados",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
            )
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                DatoResumen("${historial.size}", "traslados", Modifier.weight(1f))
                DatoResumen(
                    if (historial.isEmpty()) "–" else "${minutosCortos(promedioS)} min",
                    "promedio",
                    Modifier.weight(1f),
                )
                DatoResumen(formatearKmCorto(kmTotales), "recorridos", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DatoResumen(valor: String, etiqueta: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(valor, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimary)
        Text(
            etiqueta,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
        )
    }
}

@Composable
private fun FilaTraslado(traslado: Traslado) {
    // Cuanto alargo el trafico real este traslado respecto al tiempo libre.
    val demoraS = traslado.tiempoSConTrafico - traslado.tiempoS
    val conDemora = demoraS >= 30
    val colorDemora = if (conDemora) ApioRojoAlerta else ApioVerde

    Column {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    IconosApio.Ruta,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Ambulancia → ${traslado.destino}",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val detalle = listOfNotNull(
                    horaLocal(traslado.fechaMs),
                    "${minutosCortos(traslado.tiempoSConTrafico)} min",
                    traslado.privilegioPrincipal?.lowercase(),
                ).joinToString(" · ")
                Text(
                    detalle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!conDemora) {
                    Icon(
                        IconosApio.TendenciaBaja,
                        contentDescription = null,
                        tint = colorDemora,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    if (conDemora) "+${formatearDecimal(demoraS / 60, 1)} min" else "Fluido",
                    style = MaterialTheme.typography.labelMedium,
                    color = colorDemora,
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 52.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

@Composable
private fun EstadoVacio() {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            IconosApio.Historial,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(40.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text("Todavía no hay traslados", style = MaterialTheme.typography.titleMedium)
        Text(
            "Cada ruta que calcules a un hospital se guarda aquí.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
