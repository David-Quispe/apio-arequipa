package pe.apio.mobile.ui.pantallas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import pe.apio.mobile.ia.NivelRiesgo
import pe.apio.mobile.mapa.MapaApio
import pe.apio.mobile.modelo.Hospital
import pe.apio.mobile.modelo.LatLon
import pe.apio.mobile.plataforma.MantenerPantallaEncendida
import pe.apio.mobile.plataforma.rememberLocutor
import pe.apio.mobile.ui.EstadoPantalla
import pe.apio.mobile.ui.HojaRuta
import pe.apio.mobile.ui.IconosApio
import pe.apio.mobile.viewmodel.MapaUiState

// Cuanto dura en pantalla un aviso del modo conduccion antes de cerrarse solo.
private const val DURACION_ALERTA_MS = 5_000L
// El aviso de llegada se queda un poco mas
private const val DURACION_LLEGADA_MS = 7_000L

/** Aviso corto del modo conduccion (cambio de riesgo / trafico / llegada). */
private data class AlertaConduccion(val texto: String, val grave: Boolean, val llegada: Boolean = false)

/** El aviso que corresponde al pasar de un nivel de riesgo a otro; null si
 * no vale la pena distraer a quien maneja. */
private fun alertaPorRiesgo(anterior: NivelRiesgo?, actual: NivelRiesgo): AlertaConduccion? = when {
    actual == NivelRiesgo.ALTO ->
        AlertaConduccion("Riesgo de demora alto: tráfico fuerte en la ruta", grave = true)
    // al empezar a navegar solo se avisa si el riesgo es alto
    anterior == null -> null
    actual.ordinal > anterior.ordinal ->
        AlertaConduccion("El tráfico aumentó: riesgo de demora moderado", grave = false)
    actual == NivelRiesgo.MEDIO ->
        AlertaConduccion("El tráfico mejoró: riesgo de demora moderado", grave = false)
    else -> AlertaConduccion("El tráfico mejoró: riesgo de demora bajo", grave = false)
}

/**
 * Pantalla "01 · Mapa — Ruta sugerida" del diseno de Figma: mapa a pantalla
 * completa, buscador arriba, FAB de ubicacion y la hoja de ruta abajo. Recibe
 * el estado ya listo del ViewModel y solo reenvia eventos.
 *
 * Con ruta activa y GPS real entra en modo conduccion: se ocultan el buscador
 * y los filtros, la hoja se compacta (ETA grande + "Terminar") y los cambios
 * de riesgo se avisan arriba con una tarjeta que se cierra sola.
 * [temaOscuro] elige el estilo nocturno del mapa.
 */
@Composable
fun PantallaMapa(
    uiState: MapaUiState,
    hospitales: List<Hospital>,
    seguimientoActivo: Boolean,
    onToggleSeguimiento: () -> Unit,
    onTapMapa: (LatLon) -> Unit,
    onTapHospital: (Hospital) -> Unit,
    onCerrarRuta: () -> Unit,
    onReintentar: (Hospital) -> Unit,
    onBuscar: () -> Unit,
    modifier: Modifier = Modifier,
    temaOscuro: Boolean = false,
    onLlegadaVista: () -> Unit = {},
) {
    val ruta = (uiState.estado as? EstadoPantalla.Exito)?.ruta

    // Modo navegacion: hay una ruta y el GPS real esta moviendo la ambulancia.
    // El mapa pasa a camara inclinada que la sigue, como Google Maps.
    val modoNavegacion = ruta != null && seguimientoActivo
    // Pasa a false cuando el usuario arrastra el mapa para mirar otra zona;
    // el boton "Recentrar" lo devuelve a true.
    var siguiendoCamara by remember { mutableStateOf(true) }
    LaunchedEffect(modoNavegacion) { siguiendoCamara = true }

    // Navegando la pantalla no se apaga y los avisos tambien se dicen en voz
    // alta, para no tener que mirar el celular.
    MantenerPantallaEncendida(modoNavegacion)
    val locutor = rememberLocutor()
    LaunchedEffect(modoNavegacion) {
        if (modoNavegacion && ruta != null) {
            val minutos = kotlin.math.round(ruta.tiempoSConTrafico / 60).toInt().coerceAtLeast(1)
            locutor.decir("Ruta iniciada hacia ${ruta.destino.nombre}. Llegada estimada en $minutos minutos.")
        }
    }

    // Llegada al hospital (la detecta el ViewModel con el GPS real): se apaga
    // el seguimiento, se avisa y el aviso se cierra solo.
    val llegadaA = uiState.llegadaA
    LaunchedEffect(llegadaA) {
        if (llegadaA != null) {
            if (seguimientoActivo) onToggleSeguimiento()
            locutor.decir("Llegaste a $llegadaA. Traslado terminado.")
            delay(DURACION_LLEGADA_MS)
            onLlegadaVista()
        }
    }

    // Alto de la hoja inferior: el mapa lo usa para no dejar la ambulancia
    // ni la ruta escondidas detras de ella.
    var altoHojaPx by remember { mutableIntStateOf(0) }

    // Avisos del modo conduccion: aparecen cuando cambia el nivel de riesgo
    // (que sale del trafico en vivo) y se cierran solos.
    var alerta by remember { mutableStateOf<AlertaConduccion?>(null) }
    val nivelRiesgo = uiState.riesgo?.nivel
    val nivelVisto = remember { arrayOfNulls<NivelRiesgo>(1) }
    LaunchedEffect(modoNavegacion, nivelRiesgo) {
        val anterior = nivelVisto[0]
        nivelVisto[0] = if (modoNavegacion) nivelRiesgo else null
        if (!modoNavegacion) {
            alerta = null
        } else if (nivelRiesgo != null && nivelRiesgo != anterior) {
            alertaPorRiesgo(anterior, nivelRiesgo)?.let { alerta = it }
        }
    }
    LaunchedEffect(alerta) {
        val aviso = alerta
        if (aviso != null) {
            locutor.decir(aviso.texto)
            delay(DURACION_ALERTA_MS)
            alerta = null
        }
    }

    // Por defecto el mapa muestra solo los destinos con emergencia; "Todos"
    // suma los puestos y centros de salud de primer nivel (en gris).
    var verTodos by rememberSaveable { mutableStateOf(false) }
    val totalEmergencia = remember(hospitales) { hospitales.count { it.emergencia } }
    val hospitalesMapa = remember(hospitales, verTodos) {
        if (verTodos) hospitales else hospitales.filter { it.emergencia }
    }

    Box(modifier.fillMaxSize()) {
        MapaApio(
            modifier = Modifier.fillMaxSize(),
            origen = uiState.origen,
            rumbo = uiState.rumbo,
            hospitales = hospitalesMapa,
            rutaPuntos = ruta?.puntos ?: emptyList(),
            modoNavegacion = modoNavegacion,
            siguiendoCamara = siguiendoCamara,
            oscuro = temaOscuro,
            margenInferiorPx = altoHojaPx,
            // Navegando, la posicion la manda el GPS: un toque accidental en
            // el mapa no debe mover el origen ni borrar la ruta.
            onTapMapa = { punto -> if (!modoNavegacion) onTapMapa(punto) },
            onTapHospital = onTapHospital,
            onCamaraMovidaPorUsuario = { siguiendoCamara = false },
        )

        if (llegadaA != null) {
            TarjetaAlerta(
                AlertaConduccion("Llegaste a $llegadaA", grave = false, llegada = true),
                Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth(),
            )
        } else if (modoNavegacion) {
            // Manejando no hay buscador ni filtros: solo, cuando hace falta,
            // un aviso arriba que no tapa la ruta.
            alerta?.let { aviso ->
                TarjetaAlerta(
                    aviso,
                    Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                )
            }
        } else Column(
            Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth(),
        ) {
            BarraBusqueda(onClick = onBuscar, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChipCapa(
                    texto = "Con emergencia ($totalEmergencia)",
                    seleccionado = !verTodos,
                    onClick = { verTodos = false },
                )
                ChipCapa(
                    texto = "Todos (${hospitales.size})",
                    seleccionado = verTodos,
                    onClick = { verTodos = true },
                )
            }
        }

        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
            if (modoNavegacion && !siguiendoCamara) {
                // Grande a proposito: se toca manejando
                ExtendedFloatingActionButton(
                    onClick = { siguiendoCamara = true },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 12.dp)
                        .height(64.dp),
                    icon = { Icon(IconosApio.Navegacion, contentDescription = null) },
                    text = { Text("Recentrar", style = MaterialTheme.typography.titleMedium) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            }
            // En modo conduccion el GPS se apaga con "Terminar"
            if (!modoNavegacion) FloatingActionButton(
                onClick = onToggleSeguimiento,
                modifier = Modifier.align(Alignment.End).padding(end = 16.dp, bottom = 12.dp),
                containerColor = if (seguimientoActivo) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                contentColor = if (seguimientoActivo) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.primary
                },
            ) {
                Icon(
                    IconosApio.MiUbicacion,
                    contentDescription = if (seguimientoActivo) "Apagar GPS real" else "Usar GPS real",
                )
            }
            HojaRuta(
                estado = uiState.estado,
                riesgo = uiState.riesgo,
                recomendacion = uiState.recomendacion,
                trafico = uiState.trafico,
                seguimientoActivo = seguimientoActivo,
                onToggleSeguimiento = onToggleSeguimiento,
                onCerrar = onCerrarRuta,
                onReintentar = onReintentar,
                onVerHospitales = onBuscar,
                modifier = Modifier.onSizeChanged { altoHojaPx = it.height },
                modoNavegacion = modoNavegacion,
                // terminar el traslado: se apaga el GPS real y se cierra la ruta
                onTerminar = {
                    onToggleSeguimiento()
                    onCerrarRuta()
                },
            )
        }
    }
}

/** Aviso temporal del modo conduccion (se cierra solo, ver [DURACION_ALERTA_MS]). */
@Composable
private fun TarjetaAlerta(alerta: AlertaConduccion, modifier: Modifier = Modifier) {
    val fondo = if (alerta.grave) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val contenido = if (alerta.grave) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = fondo,
        contentColor = contenido,
        shadowElevation = 6.dp,
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                when {
                    alerta.llegada -> IconosApio.Hospital
                    alerta.grave -> IconosApio.Alerta
                    else -> IconosApio.Trafico
                },
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(alerta.texto, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChipCapa(texto: String, seleccionado: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = seleccionado,
        onClick = onClick,
        label = { Text(texto) },
        leadingIcon = if (seleccionado) {
            { Icon(IconosApio.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
        } else {
            null
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        elevation = FilterChipDefaults.filterChipElevation(elevation = 3.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BarraBusqueda(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 4.dp,
    ) {
        Row(
            Modifier.height(56.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                IconosApio.Hospital,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                "¿A qué hospital vamos?",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Icon(
                IconosApio.Buscar,
                contentDescription = "Buscar hospital",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
