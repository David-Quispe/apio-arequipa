package pe.apio.mobile.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Tarjeta flotante "glass" compartida por [PanelInfo] y [PanelTrafico]: fondo
 * blanco casi opaco, esquinas muy redondeadas, sombra suave para separarla
 * del mapa. No aplica blur real del mapa de fondo -- el backdrop blur nativo
 * requiere API 31+ y esta app soporta minSdk 24, así que para esta demo se
 * aproxima el efecto "glass" solo con la opacidad alta.
 */
@Composable
fun PanelFlotante(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.94f),
        shadowElevation = 6.dp,
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}
