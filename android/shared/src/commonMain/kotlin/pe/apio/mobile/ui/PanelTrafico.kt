package pe.apio.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import pe.apio.mobile.modelo.AvenidaTrafico
import pe.apio.mobile.ui.theme.ApioAmbar
import pe.apio.mobile.ui.theme.ApioGrisSinDatos
import pe.apio.mobile.ui.theme.ApioRojoAlerta
import pe.apio.mobile.ui.theme.ApioVerde

/** Color por nivel de trafico de TomTom (mismos niveles que la PWA). */
fun colorNivelTrafico(nivel: String): Color = when (nivel) {
    "baja" -> ApioVerde
    "media" -> ApioAmbar
    "alta" -> ApioRojoAlerta
    else -> ApioGrisSinDatos
}

fun nivelTraficoLegible(nivel: String): String = when (nivel) {
    "baja" -> "Fluido"
    "media" -> "Moderado"
    "alta" -> "Pesado"
    else -> "Sin datos"
}

/**
 * Seccion de trafico dentro de la hoja de ruta. Sin ruta activa
 * (`avenidasRuta == null`) lista todas las avenidas monitoreadas; con una
 * ruta, solo las que esa ruta cruza de verdad.
 */
@Composable
fun SeccionTrafico(
    avenidas: List<AvenidaTrafico>,
    avenidasRuta: List<String>?,
    modifier: Modifier = Modifier,
) {
    val mostradas = if (avenidasRuta == null) avenidas else avenidas.filter { it.avenida in avenidasRuta }

    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                IconosApio.Trafico,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (avenidasRuta == null) "Tráfico en vivo" else "Tráfico de tu ruta",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            Text(
                "TomTom · cada 25 s",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.size(8.dp))
        when {
            avenidas.isEmpty() -> Text(
                "Esperando datos de tráfico…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            mostradas.isEmpty() -> Text(
                "Esta ruta no pasa por avenidas monitoreadas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            else -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                mostradas.forEach { avenida -> FilaAvenida(avenida) }
            }
        }
    }
}

@Composable
private fun FilaAvenida(avenida: AvenidaTrafico) {
    val color = colorNivelTrafico(avenida.nivel)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Spacer(Modifier.width(10.dp))
        Text(
            avenida.avenida,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            nivelTraficoLegible(avenida.nivel),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier
                .background(color.copy(alpha = 0.12f), CircleShape)
                .padding(horizontal = 10.dp, vertical = 2.dp),
        )
    }
}
