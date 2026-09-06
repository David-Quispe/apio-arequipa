package pe.apio.mobile.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Mismo panel que frontend/src/App.jsx: destino, distancia, ETA base
 * tachado -> ETA con trafico en negrita, y la linea de privilegios
 * ("via Av. X (Tipo N/10), ...").
 */
@Composable
fun PanelInfo(
    cargando: Boolean,
    error: String?,
    ruta: EstadoRuta?,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(Modifier.padding(12.dp)) {
            Text("APIO — demo de ruteo", fontWeight = FontWeight.Bold)
            Text(
                "Tocá el mapa para mover el origen. Tocá un hospital para calcular la ruta.",
                fontSize = 12.sp,
                color = Color.Gray,
            )
            if (cargando) {
                Text("Calculando ruta...")
            }
            if (error != null) {
                Text(error, color = Color(0xFFDC2626))
            }
            if (ruta != null && !cargando) {
                Text(ruta.destino.nombre, fontWeight = FontWeight.Bold)
                Text(formatearDistanciaKm(ruta.distanciaM))
                Row {
                    Text(
                        formatearMinutos(ruta.tiempoS),
                        textDecoration = TextDecoration.LineThrough,
                        color = Color.Gray,
                    )
                    Text(" → ")
                    Text(formatearMinutos(ruta.tiempoSConTrafico), fontWeight = FontWeight.Bold)
                    Text(" con tráfico actual")
                    Text(" (se actualiza solo)", fontSize = 12.sp, color = Color.Gray)
                }
                if (ruta.privilegiosCruzados.isNotEmpty()) {
                    Text(
                        "vía ${formatearPrivilegios(ruta.privilegiosCruzados)}",
                        fontSize = 12.sp,
                        color = Color.Gray,
                    )
                }
            }
        }
    }
}
