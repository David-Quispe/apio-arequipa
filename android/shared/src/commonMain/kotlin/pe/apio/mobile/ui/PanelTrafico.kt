package pe.apio.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.apio.mobile.modelo.AvenidaTrafico

// Mismos colores que NIVEL_COLORES en frontend/src/App.jsx.
private val COLOR_POR_NIVEL = mapOf(
    "baja" to Color(0xFF059669),
    "media" to Color(0xFFD97706),
    "alta" to Color(0xFFDC2626),
    "sin_datos" to Color(0xFF9CA3AF),
)

/**
 * Panel de tráfico. Sin ruta activa (`avenidasRuta == null`) muestra TODAS
 * las avenidas monitoreadas (16 desde que se amplió el area piloto a toda
 * Arequipa, ver corridor_overlap.CASE_AVENIDA_TRAFICO). Con una ruta
 * calculada se filtra a solo las que ESA ruta cruza de verdad -- una ruta
 * corta suele no tocar ninguna, ver memoria del proyecto, así que ese caso
 * muestra un texto en vez de una lista vacía. La lista va en una columna
 * con scroll propio y alto máximo para que 16 avenidas no se salgan de la
 * pantalla (con una ruta activa, filtrada, suele ser mucho más corta).
 */
@Composable
fun PanelTrafico(
    avenidas: List<AvenidaTrafico>,
    avenidasRuta: List<String>?,
    modifier: Modifier = Modifier,
) {
    val mostradas = if (avenidasRuta == null) avenidas else avenidas.filter { it.avenida in avenidasRuta }

    PanelFlotante(modifier = modifier) {
        Text(
            if (avenidasRuta == null) "Tráfico en vivo" else "Tráfico de tu ruta",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
        )
        Text("Se sondea cada 25s", fontSize = 11.sp, color = Color.Gray)
        Spacer(Modifier.height(8.dp))
        if (avenidasRuta != null && mostradas.isEmpty()) {
            Text(
                "Esta ruta no pasa por ninguna avenida con datos de tráfico",
                fontSize = 12.sp,
                color = Color.Gray,
            )
        }
        Column(
            Modifier
                .heightIn(max = 220.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            mostradas.forEach { avenida ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .background(COLOR_POR_NIVEL[avenida.nivel] ?: Color.Gray, CircleShape)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(avenida.avenida, fontSize = 13.sp)
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}
