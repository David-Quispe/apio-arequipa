package pe.apio.mobile.ui.pantallas

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pe.apio.mobile.ui.HaloPulsante
import pe.apio.mobile.ui.theme.ApioRojoAlerta
import pe.apio.mobile.ui.theme.AzulBienvenidaAbajo
import pe.apio.mobile.ui.theme.AzulBienvenidaArriba
import pe.apio.mobile.ui.theme.AzulPrimario

/**
 * Pantalla de bienvenida que se ve un par de segundos al abrir la app: el
 * emblema entra con un rebote, una linea de pulso se dibuja de izquierda a
 * derecha y aparece el nombre. Usa siempre los azules de la marca (no
 * cambia con el tema) y [App] la desvanece sobre el mapa ya cargado.
 */
@Composable
fun PantallaBienvenida(modifier: Modifier = Modifier) {
    val emblema = remember { Animatable(0f) }
    val pulso = remember { Animatable(0f) }
    val titulo = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            emblema.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow))
        }
        launch {
            delay(300)
            pulso.animateTo(1f, tween(durationMillis = 1100, easing = FastOutSlowInEasing))
        }
        launch {
            delay(500)
            titulo.animateTo(1f, tween(durationMillis = 600, easing = FastOutSlowInEasing))
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(AzulBienvenidaArriba, AzulPrimario, AzulBienvenidaAbajo)))
            // se queda con los toques: nada de lo de abajo se puede tocar todavia
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(184.dp), contentAlignment = Alignment.Center) {
                HaloPulsante(Color.White, Modifier.fillMaxSize())
                Box(
                    Modifier
                        .size(108.dp)
                        .graphicsLayer {
                            scaleX = emblema.value
                            scaleY = emblema.value
                        }
                        .background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    // cruz roja dibujada a mano (dos barras redondeadas)
                    Canvas(Modifier.size(54.dp)) {
                        val grosor = size.width * 0.30f
                        val esquina = CornerRadius(grosor * 0.2f)
                        drawRoundRect(
                            color = ApioRojoAlerta,
                            topLeft = Offset((size.width - grosor) / 2f, 0f),
                            size = Size(grosor, size.height),
                            cornerRadius = esquina,
                        )
                        drawRoundRect(
                            color = ApioRojoAlerta,
                            topLeft = Offset(0f, (size.height - grosor) / 2f),
                            size = Size(size.width, grosor),
                            cornerRadius = esquina,
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            LineaDePulso(avance = pulso.value, modifier = Modifier.width(230.dp).height(46.dp))
            Spacer(Modifier.height(20.dp))

            Column(
                Modifier.graphicsLayer {
                    alpha = titulo.value
                    translationY = (1f - titulo.value) * 24.dp.toPx()
                },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "APIO",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 8.sp,
                    color = Color.White,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Rutas rápidas para ambulancias · Arequipa",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }
        }
    }
}

/**
 * Linea tipo electrocardiograma que se va dibujando: [avance] de 0 (nada) a
 * 1 (completa). Se arma el trazo entero y se pinta solo el tramo que toca.
 */
@Composable
private fun LineaDePulso(avance: Float, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val medio = h * 0.55f
        val trazo = Path().apply {
            moveTo(0f, medio)
            lineTo(w * 0.30f, medio)
            lineTo(w * 0.36f, medio - h * 0.18f)
            lineTo(w * 0.41f, medio)
            lineTo(w * 0.46f, medio)
            lineTo(w * 0.51f, h * 0.04f)
            lineTo(w * 0.57f, h * 0.98f)
            lineTo(w * 0.62f, medio)
            lineTo(w * 0.68f, medio)
            lineTo(w * 0.74f, medio - h * 0.24f)
            lineTo(w * 0.80f, medio)
            lineTo(w, medio)
        }
        val medida = PathMeasure()
        medida.setPath(trazo, false)
        val visible = Path()
        medida.getSegment(0f, medida.length * avance.coerceIn(0f, 1f), visible, true)
        drawPath(
            path = visible,
            color = Color.White,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}
