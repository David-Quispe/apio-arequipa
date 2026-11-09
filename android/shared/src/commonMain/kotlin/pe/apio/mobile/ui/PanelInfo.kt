package pe.apio.mobile.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.apio.mobile.ia.EvaluacionRiesgo
import pe.apio.mobile.ia.NivelRiesgo
import pe.apio.mobile.modelo.Hospital
import pe.apio.mobile.modelo.PrivilegioCruzado

// Ambar oscuro para alertas -- deliberadamente distinto del rojo de marca
// (#e11d48, usado por la ruta) y del ambar de trafico "medio" (#d97706), para
// que un error nunca se lea como "hay una ruta trazada" ni como "trafico
// medio en una avenida".
private val COLOR_ALERTA = Color(0xFFB45309)
private val COLOR_ETA_ACTUAL = Color(0xFF1F2937)

/**
 * Panel flotante superior. Un estado, una tarjeta: mapea 1:1 con
 * [EstadoPantalla] (vacío / cargando / éxito / error) en vez de combinar
 * varias banderas sueltas como antes.
 */
@Composable
fun PanelInfo(
    estado: EstadoPantalla,
    riesgo: EvaluacionRiesgo?,
    onDismiss: () -> Unit,
    onRetry: (Hospital) -> Unit,
    modifier: Modifier = Modifier,
) {
    PanelFlotante(modifier = modifier) {
        when (estado) {
            is EstadoPantalla.Vacio -> VacioCard()
            is EstadoPantalla.Cargando -> CargandoCard(estado)
            is EstadoPantalla.Error -> ErrorCard(estado, onDismiss, onRetry)
            is EstadoPantalla.Exito -> ExitoCard(estado.ruta, riesgo, onDismiss)
        }
    }
}

@Composable
private fun VacioCard() {
    Text("Toca un hospital para trazar la ruta", fontWeight = FontWeight.Bold, fontSize = 16.sp)
}

@Composable
private fun CargandoCard(estado: EstadoPantalla.Cargando) {
    Text(estado.hospital.nombre, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    Spacer(Modifier.height(10.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
        Spacer(Modifier.width(10.dp))
        Text("Calculando ruta…", color = Color.Gray)
    }
}

@Composable
private fun ErrorCard(
    estado: EstadoPantalla.Error,
    onDismiss: () -> Unit,
    onRetry: (Hospital) -> Unit,
) {
    // El backend distingue este caso con un mensaje exacto (ver
    // project_apio_corridor): reubicarse es la unica solucion, reintentar
    // con el mismo origen no cambia nada, asi que ese boton no aplica aca.
    val esFueraDeArea = estado.mensaje.contains("fuera del área piloto")

    Text("⚠️", fontSize = 30.sp)
    Spacer(Modifier.height(6.dp))
    Text(estado.mensaje, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = COLOR_ALERTA)
    Spacer(Modifier.height(4.dp))
    Text(
        if (esFueraDeArea) {
            "La ambulancia debe estar dentro del corredor de cobertura (Cono Norte: Cayma, Yanahuara, Cerro Colorado)."
        } else {
            "El servicio de rutas no está disponible en este momento. Intenta de nuevo en unos segundos."
        },
        fontSize = 13.sp,
        color = Color.Gray,
    )
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!esFueraDeArea) {
            Button(onClick = { onRetry(estado.hospital) }) { Text("Reintentar") }
        }
        OutlinedButton(onClick = onDismiss) { Text("Cerrar") }
    }
}

@Composable
private fun ExitoCard(ruta: EstadoRuta, riesgo: EvaluacionRiesgo?, onDismiss: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            ruta.destino.nombre,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
            Text("✕", fontSize = 16.sp, color = Color.Gray)
        }
    }
    Text(formatearDistanciaKm(ruta.distanciaM), color = Color.Gray, fontSize = 14.sp)
    Spacer(Modifier.height(4.dp))
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            formatearMinutos(ruta.tiempoS),
            textDecoration = TextDecoration.LineThrough,
            color = Color.Gray,
            fontSize = 15.sp,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            formatearMinutos(ruta.tiempoSConTrafico),
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            color = COLOR_ETA_ACTUAL,
        )
    }
    if (ruta.privilegiosCruzados.isNotEmpty()) {
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ruta.privilegiosCruzados.forEach { privilegio -> ChipPrivilegio(privilegio) }
        }
    }
    if (riesgo != null) {
        BannerRiesgo(riesgo)
    }
    Spacer(Modifier.height(10.dp))
    IndicadorAutoActualiza()
}

/** Recomendacion de la heuristica IA (ver pe.apio.mobile.ia.EstimadorRiesgo)
 * para la ruta que se esta mostrando. */
@Composable
private fun BannerRiesgo(riesgo: EvaluacionRiesgo) {
    val color = when (riesgo.nivel) {
        NivelRiesgo.BAJO -> Color(0xFF059669)
        NivelRiesgo.MEDIO -> Color(0xFFD97706)
        NivelRiesgo.ALTO -> Color(0xFFDC2626)
    }
    Spacer(Modifier.height(10.dp))
    Surface(shape = RoundedCornerShape(10.dp), color = color.copy(alpha = 0.12f)) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(8.dp).background(color, CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(riesgo.mensaje, fontSize = 12.sp, color = color, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun ChipPrivilegio(privilegio: PrivilegioCruzado) {
    val tipo = privilegio.tipoPrincipal ?: "Privilegio"
    val score = privilegio.tipoPrincipalScore ?: privilegio.general
    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFF3F4F6)) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text(
                "${privilegio.avenida} · $tipo ${formatearDecimal(score, 1)}/10",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { (score / 10.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.width(90.dp).height(4.dp),
                color = Color(0xFFE11D48),
                trackColor = Color(0xFFE5E7EB),
            )
        }
    }
}

@Composable
private fun IndicadorAutoActualiza() {
    val transicion = rememberInfiniteTransition(label = "pulso")
    val alfa by transicion.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "alfaPulso",
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(8.dp)
                .background(Color(0xFF059669).copy(alpha = alfa), CircleShape)
        )
        Spacer(Modifier.width(6.dp))
        Text("Se actualiza solo", fontSize = 11.sp, color = Color.Gray)
    }
}
