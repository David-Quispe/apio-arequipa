package pe.apio.mobile.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

// Animaciones chicas que comparten las pantallas, para que toda la app se
// mueva con el mismo estilo (mismas duraciones y la misma curva).

/**
 * Entrada suave: el elemento aparece subiendo unos dp mientras se aclara.
 * [orden] escalona varios elementos de una misma pantalla (0 entra primero).
 */
@Composable
fun Modifier.entradaSuave(orden: Int = 0): Modifier {
    val progreso = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(orden.coerceIn(0, 8) * 55L)
        progreso.animateTo(1f, tween(durationMillis = 420, easing = FastOutSlowInEasing))
    }
    return graphicsLayer {
        alpha = progreso.value
        translationY = (1f - progreso.value) * 28.dp.toPx()
    }
}

/** Un numero que "cuenta" desde su valor anterior hasta [objetivo]. */
@Composable
fun numeroAnimado(objetivo: Float): Float {
    val valor = remember { Animatable(0f) }
    LaunchedEffect(objetivo) {
        valor.animateTo(objetivo, tween(durationMillis = 800, easing = FastOutSlowInEasing))
    }
    return valor.value
}

/**
 * Dos anillos que se expanden y se desvanecen, como un pulso. Se dibuja
 * detras de algo que esta "activo" (el GPS prendido, el logo al abrir).
 */
@Composable
fun HaloPulsante(color: Color, modifier: Modifier = Modifier) {
    val transicion = rememberInfiniteTransition(label = "halo")
    val fase by transicion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label = "faseHalo",
    )
    Canvas(modifier) {
        val radioMaximo = size.minDimension / 2f
        for (desfase in listOf(0f, 0.5f)) {
            val f = (fase + desfase) % 1f
            drawCircle(
                color = color.copy(alpha = (1f - f) * 0.35f),
                radius = radioMaximo * (0.62f + 0.38f * f),
                style = Stroke(width = 2.dp.toPx()),
            )
        }
    }
}
