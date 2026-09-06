package pe.apio.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
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

@Composable
fun PanelTrafico(avenidas: List<AvenidaTrafico>, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.padding(12.dp)) {
            Text("Tráfico del corredor", fontWeight = FontWeight.Bold)
            avenidas.forEach { avenida ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .background(COLOR_POR_NIVEL[avenida.nivel] ?: Color.Gray, CircleShape)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(avenida.avenida, fontSize = 12.sp)
                }
            }
        }
    }
}
