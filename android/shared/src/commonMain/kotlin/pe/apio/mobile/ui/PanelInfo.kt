package pe.apio.mobile.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pe.apio.mobile.ia.EvaluacionRiesgo
import pe.apio.mobile.ia.NivelRiesgo
import pe.apio.mobile.modelo.AvenidaTrafico
import pe.apio.mobile.modelo.Hospital
import pe.apio.mobile.modelo.PrivilegioCruzado
import pe.apio.mobile.ui.theme.ApioAmbar
import pe.apio.mobile.ui.theme.ApioRojoAlerta
import pe.apio.mobile.ui.theme.ApioVerde
import pe.apio.mobile.viewmodel.EstadoRecomendacion

/**
 * Hoja inferior de la pantalla del mapa (el "Bottom sheet · Ruta" del diseno
 * de Figma). Reemplaza al panel flotante de arriba que tapaba medio mapa.
 * Solo DIBUJA el estado que le pasa el ViewModel: un estado de
 * [EstadoPantalla], una vista. Tocar el asa (o deslizarla) la expande o
 * contrae para mostrar u ocultar los detalles (privilegios y trafico).
 *
 * Con [modoNavegacion] (ruta activa + GPS real) pasa a modo conduccion: una
 * franja compacta con el ETA en grande y un unico boton "Terminar", para
 * leer de un vistazo y no tapar el mapa. El detalle queda al expandirla.
 */
@Composable
fun HojaRuta(
    estado: EstadoPantalla,
    riesgo: EvaluacionRiesgo?,
    recomendacion: EstadoRecomendacion?,
    trafico: List<AvenidaTrafico>,
    seguimientoActivo: Boolean,
    onToggleSeguimiento: () -> Unit,
    onCerrar: () -> Unit,
    onReintentar: (Hospital) -> Unit,
    onVerHospitales: () -> Unit,
    modifier: Modifier = Modifier,
    modoNavegacion: Boolean = false,
    onTerminar: () -> Unit = {},
) {
    var expandida by rememberSaveable { mutableStateOf(false) }
    // al empezar a navegar la hoja arranca compacta, aunque estuviera abierta
    LaunchedEffect(modoNavegacion) { if (modoNavegacion) expandida = false }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = 8.dp,
    ) {
        Column(Modifier.fillMaxWidth().animateContentSize()) {
            Asa(
                onClick = { expandida = !expandida },
                onDeslizar = { haciaArriba -> expandida = haciaArriba },
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 470.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
            ) {
                when (estado) {
                    is EstadoPantalla.Vacio -> ContenidoVacio(trafico, expandida, onVerHospitales)
                    is EstadoPantalla.Cargando -> ContenidoCargando(estado.hospital)
                    is EstadoPantalla.Error -> ContenidoError(estado, onCerrar, onReintentar)
                    is EstadoPantalla.Exito -> if (modoNavegacion) {
                        ContenidoNavegacion(
                            ruta = estado.ruta,
                            riesgo = riesgo,
                            recomendacion = recomendacion,
                            trafico = trafico,
                            expandida = expandida,
                            onTerminar = onTerminar,
                        )
                    } else ContenidoRuta(
                        ruta = estado.ruta,
                        riesgo = riesgo,
                        recomendacion = recomendacion,
                        trafico = trafico,
                        expandida = expandida,
                        seguimientoActivo = seguimientoActivo,
                        onToggleSeguimiento = onToggleSeguimiento,
                        onCerrar = onCerrar,
                        onVerDetalles = { expandida = true },
                    )
                }
            }
        }
    }
}

@Composable
private fun Asa(onClick: () -> Unit, onDeslizar: (haciaArriba: Boolean) -> Unit) {
    val deslizar by rememberUpdatedState(onDeslizar)
    Box(
        Modifier
            .fillMaxWidth()
            // deslizar hacia arriba abre el detalle y hacia abajo lo cierra
            .pointerInput(Unit) {
                val umbral = 24.dp.toPx()
                var recorrido = 0f
                detectVerticalDragGestures(
                    onDragStart = { recorrido = 0f },
                    onDragEnd = {
                        if (recorrido < -umbral) deslizar(true)
                        if (recorrido > umbral) deslizar(false)
                    },
                ) { _, delta -> recorrido += delta }
            }
            .clickable(onClick = onClick)
            .padding(top = 12.dp, bottom = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(width = 32.dp, height = 4.dp)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(2.dp)),
        )
    }
}

// ---------------------------------------------------------------- Vacio

@Composable
private fun ContenidoVacio(trafico: List<AvenidaTrafico>, expandida: Boolean, onVerHospitales: () -> Unit) {
    Text(
        "Sin ruta activa",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text("¿A qué hospital vamos?", style = MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(4.dp))
    Text(
        "Toca un hospital en el mapa o elígelo de la lista para calcular la ruta más rápida.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(16.dp))
    Button(
        onClick = onVerHospitales,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(28.dp),
    ) {
        Icon(IconosApio.Hospital, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text("Elegir hospital")
    }
    if (expandida) {
        Spacer(Modifier.height(20.dp))
        SeccionTrafico(avenidas = trafico, avenidasRuta = null)
    }
}

// ---------------------------------------------------------------- Cargando

@Composable
private fun ContenidoCargando(hospital: Hospital) {
    Text(
        "Calculando la ruta más rápida",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(hospital.nombre, style = MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(16.dp))
    LinearProgressIndicator(Modifier.fillMaxWidth().height(6.dp))
    Spacer(Modifier.height(10.dp))
    Text(
        "Considerando contraflujo, carril exclusivo y tráfico en vivo…",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

// ---------------------------------------------------------------- Error

@Composable
private fun ContenidoError(
    estado: EstadoPantalla.Error,
    onCerrar: () -> Unit,
    onReintentar: (Hospital) -> Unit,
) {
    // El backend distingue este caso con un mensaje exacto: reubicarse es la
    // unica solucion, asi que reintentar no aplica.
    val esFueraDeArea = estado.mensaje.contains("fuera del área piloto")

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(40.dp).background(MaterialTheme.colorScheme.errorContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                IconosApio.Alerta,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            estado.mensaje,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f),
        )
    }
    Spacer(Modifier.height(8.dp))
    Text(
        if (esFueraDeArea) {
            "La ambulancia debe estar dentro del corredor de cobertura (Cono Norte: Cayma, Yanahuara, Cerro Colorado)."
        } else {
            "El servicio de rutas no está disponible en este momento. Intenta de nuevo en unos segundos."
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(16.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!esFueraDeArea) {
            Button(onClick = { onReintentar(estado.hospital) }, shape = RoundedCornerShape(28.dp)) {
                Text("Reintentar")
            }
        }
        OutlinedButton(onClick = onCerrar, shape = RoundedCornerShape(28.dp)) { Text("Cerrar") }
    }
}

// ---------------------------------------------------------------- Ruta

@Composable
private fun ContenidoRuta(
    ruta: EstadoRuta,
    riesgo: EvaluacionRiesgo?,
    recomendacion: EstadoRecomendacion?,
    trafico: List<AvenidaTrafico>,
    expandida: Boolean,
    seguimientoActivo: Boolean,
    onToggleSeguimiento: () -> Unit,
    onCerrar: () -> Unit,
    onVerDetalles: () -> Unit,
) {
    // ETA grande + chip de trafico
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            Text(
                "Llegada estimada",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    minutosCortos(ruta.tiempoSConTrafico),
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "min",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            Text(
                "Sin tráfico: ${formatearMinutos(ruta.tiempoS)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textDecoration = TextDecoration.LineThrough,
            )
        }
        if (riesgo != null) {
            ChipNivelTrafico(riesgo.nivel, Modifier.padding(bottom = 4.dp))
        }
    }

    Spacer(Modifier.height(14.dp))
    Text(ruta.destino.nombre, style = MaterialTheme.typography.headlineSmall)
    Text(
        listOfNotNull(ruta.destino.tipo, ruta.destino.categoria, formatearDistanciaKm(ruta.distanciaM))
            .joinToString(" · "),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    if (ruta.privilegiosCruzados.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ruta.privilegiosCruzados.forEach { privilegio -> ChipPrivilegio(privilegio) }
        }
    }

    if (riesgo != null) {
        Spacer(Modifier.height(12.dp))
        BannerRiesgo(riesgo.nivel)
    }

    if (recomendacion != null && recomendacion !is EstadoRecomendacion.NoDisponible) {
        Spacer(Modifier.height(8.dp))
        TarjetaRecomendacionIa(recomendacion)
    }

    Spacer(Modifier.height(16.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Button(
            onClick = onToggleSeguimiento,
            modifier = Modifier.weight(1f).height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = if (seguimientoActivo) {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                )
            } else {
                ButtonDefaults.buttonColors()
            },
        ) {
            Icon(IconosApio.Navegacion, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (seguimientoActivo) "Detener seguimiento" else "Iniciar ruta")
        }
        Spacer(Modifier.width(12.dp))
        FilledTonalIconButton(onClick = onCerrar, modifier = Modifier.size(56.dp)) {
            Icon(IconosApio.Cerrar, contentDescription = "Cerrar ruta")
        }
    }

    Spacer(Modifier.height(12.dp))
    if (expandida) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(12.dp))
        SeccionTrafico(avenidas = trafico, avenidasRuta = ruta.avenidasTraficoCruzadas)
        Spacer(Modifier.height(12.dp))
        IndicadorAutoActualiza()
    } else {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onVerDetalles).padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IndicadorAutoActualiza(Modifier.weight(1f))
            Text(
                "Ver tráfico de la ruta",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

// ---------------------------------------------------------------- Navegacion

/**
 * Modo conduccion: lo minimo para leer manejando. ETA grande, distancia,
 * hospital y un solo boton grande. Todo lo demas (privilegios, riesgo,
 * recomendacion IA, trafico) aparece recien al expandir la hoja.
 */
@Composable
private fun ContenidoNavegacion(
    ruta: EstadoRuta,
    riesgo: EvaluacionRiesgo?,
    recomendacion: EstadoRecomendacion?,
    trafico: List<AvenidaTrafico>,
    expandida: Boolean,
    onTerminar: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    minutosCortos(ruta.tiempoSConTrafico),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "min",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 5.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "· ${formatearKmCorto(ruta.distanciaM)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 7.dp),
                )
            }
            Text(
                ruta.destino.nombre,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(12.dp))
        // 64 dp de alto: se tiene que poder tocar sin apuntar
        Button(
            onClick = onTerminar,
            modifier = Modifier.height(64.dp),
            shape = RoundedCornerShape(32.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
        ) {
            Icon(IconosApio.Cerrar, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text("Terminar", style = MaterialTheme.typography.titleMedium)
        }
    }

    if (expandida) {
        if (ruta.privilegiosCruzados.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ruta.privilegiosCruzados.forEach { privilegio -> ChipPrivilegio(privilegio) }
            }
        }
        if (riesgo != null) {
            Spacer(Modifier.height(12.dp))
            BannerRiesgo(riesgo.nivel)
        }
        if (recomendacion != null && recomendacion !is EstadoRecomendacion.NoDisponible) {
            Spacer(Modifier.height(8.dp))
            TarjetaRecomendacionIa(recomendacion)
        }
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(12.dp))
        SeccionTrafico(avenidas = trafico, avenidasRuta = ruta.avenidasTraficoCruzadas)
        Spacer(Modifier.height(12.dp))
        IndicadorAutoActualiza()
    }
}

private fun colorNivelRiesgo(nivel: NivelRiesgo): Color = when (nivel) {
    NivelRiesgo.BAJO -> ApioVerde
    NivelRiesgo.MEDIO -> ApioAmbar
    NivelRiesgo.ALTO -> ApioRojoAlerta
}

@Composable
private fun ChipNivelTrafico(nivel: NivelRiesgo, modifier: Modifier = Modifier) {
    val color = colorNivelRiesgo(nivel)
    val texto = when (nivel) {
        NivelRiesgo.BAJO -> "Tráfico fluido"
        NivelRiesgo.MEDIO -> "Tráfico moderado"
        NivelRiesgo.ALTO -> "Tráfico pesado"
    }
    Surface(modifier = modifier, shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.16f)) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(IconosApio.Trafico, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(texto, style = MaterialTheme.typography.labelLarge, color = color)
        }
    }
}

/** Resultado de la heuristica on-device (ia/EstimadorRiesgo.kt). */
@Composable
private fun BannerRiesgo(nivel: NivelRiesgo) {
    val color = colorNivelRiesgo(nivel)
    val texto = when (nivel) {
        NivelRiesgo.BAJO -> "Riesgo de demora bajo: el ETA se mantiene cerca del tiempo sin tráfico."
        NivelRiesgo.MEDIO -> "Riesgo de demora moderado: reconfirma la ruta antes de salir."
        NivelRiesgo.ALTO -> "Riesgo de demora alto: el tráfico está afectando fuerte esta ruta."
    }
    Surface(shape = RoundedCornerShape(12.dp), color = color.copy(alpha = 0.12f)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(8.dp).background(color, CircleShape))
            Spacer(Modifier.width(10.dp))
            Text(texto, style = MaterialTheme.typography.bodySmall, color = color)
        }
    }
}

/** Recomendacion del LLM (Gemini, via backend). */
@Composable
private fun TarjetaRecomendacionIa(recomendacion: EstadoRecomendacion) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    IconosApio.Ia,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Recomendación IA",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "Gemini",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(6.dp))
            when (recomendacion) {
                is EstadoRecomendacion.Cargando -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Analizando la ruta…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                is EstadoRecomendacion.Lista -> Text(
                    recomendacion.texto,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                is EstadoRecomendacion.NoDisponible -> Unit
            }
        }
    }
}

@Composable
private fun ChipPrivilegio(privilegio: PrivilegioCruzado) {
    val tipo = privilegio.tipoPrincipal ?: "Privilegio"
    val score = privilegio.tipoPrincipalScore ?: privilegio.general
    val icono = if (tipo.contains("carril", ignoreCase = true)) IconosApio.Ruta else IconosApio.Alerta
    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icono,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "$tipo ${privilegio.avenida} · ${formatearDecimal(score, 1)}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}

@Composable
private fun IndicadorAutoActualiza(modifier: Modifier = Modifier) {
    val transicion = rememberInfiniteTransition(label = "pulso")
    val alfa by transicion.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "alfaPulso",
    )
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(ApioVerde.copy(alpha = alfa), CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(
            "Se actualiza solo",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
