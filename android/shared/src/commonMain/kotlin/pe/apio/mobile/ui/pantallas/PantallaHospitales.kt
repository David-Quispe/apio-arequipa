package pe.apio.mobile.ui.pantallas

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import pe.apio.mobile.modelo.COLOR_POR_TIPO
import pe.apio.mobile.modelo.Hospital
import pe.apio.mobile.modelo.LatLon
import pe.apio.mobile.ui.EstadoPantalla
import pe.apio.mobile.ui.IconosApio
import pe.apio.mobile.ui.distanciaLineaRectaM
import pe.apio.mobile.ui.formatearKmCorto
import pe.apio.mobile.ui.minutosCortos

/** "#2563eb" -> Color. Los colores por tipo de hospital viven como texto en
 * modelo/Hospitales.kt porque los comparte el mapa nativo (MapLibre). */
internal fun colorDesdeHex(hex: String): Color =
    Color(("FF" + hex.removePrefix("#")).toLong(16))

/**
 * Pantalla "02 · Hospitales" del diseno de Figma, con datos reales: la lista
 * de hospitales del corredor, ordenada por distancia en linea recta a la
 * ambulancia, con busqueda y filtro por tipo. Elegir uno calcula la ruta
 * (evento seleccionarHospital del ViewModel) y vuelve al mapa.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaHospitales(
    hospitales: List<Hospital>,
    origen: LatLon,
    estado: EstadoPantalla,
    onElegir: (Hospital) -> Unit,
    modifier: Modifier = Modifier,
) {
    var busqueda by rememberSaveable { mutableStateOf("") }
    var filtroTipo by rememberSaveable { mutableStateOf<String?>(null) }
    var soloEmergencia by rememberSaveable { mutableStateOf(true) }

    val tipos = hospitales.map { it.tipo }.distinct()
    val destinoActivo = when (estado) {
        is EstadoPantalla.Exito -> estado.ruta.destino.nombre
        is EstadoPantalla.Cargando -> estado.hospital.nombre
        else -> null
    }
    val etaActivoS = (estado as? EstadoPantalla.Exito)?.ruta?.tiempoSConTrafico

    val lista = hospitales
        .filter { !soloEmergencia || it.emergencia }
        .filter { filtroTipo == null || it.tipo == filtroTipo }
        .filter { busqueda.isBlank() || it.nombre.contains(busqueda.trim(), ignoreCase = true) }
        .map { it to distanciaLineaRectaM(origen, it.posicion) }
        .sortedBy { it.second }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Hospitales", style = MaterialTheme.typography.headlineMedium)
        }
        item {
            OutlinedTextField(
                value = busqueda,
                onValueChange = { busqueda = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar hospital o clínica") },
                leadingIcon = { Icon(IconosApio.Buscar, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
            )
        }
        item {
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FiltroTipo("Con emergencia", seleccionado = soloEmergencia) { soloEmergencia = !soloEmergencia }
                FiltroTipo("Todos", seleccionado = filtroTipo == null) { filtroTipo = null }
                tipos.forEach { tipo ->
                    FiltroTipo(tipo, seleccionado = filtroTipo == tipo) {
                        filtroTipo = if (filtroTipo == tipo) null else tipo
                    }
                }
            }
        }
        item {
            Text(
                "${lista.size} establecimientos · ordenados por cercanía · RENIPRESS + OpenStreetMap",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // clave unica: con cientos de establecimientos de RENIPRESS hay
        // nombres repetidos, y una clave duplicada hace crashear la LazyColumn
        items(
            lista,
            key = { (h, _) -> h.codigo ?: "${h.nombre}@${h.posicion.lat},${h.posicion.lon}" },
        ) { (hospital, distanciaM) ->
            val esActivo = hospital.nombre == destinoActivo
            TarjetaHospital(
                hospital = hospital,
                distanciaM = distanciaM,
                esDestinoActivo = esActivo,
                etaS = if (esActivo) etaActivoS else null,
                onClick = { onElegir(hospital) },
            )
        }
        if (lista.isEmpty()) {
            item {
                Text(
                    "Ningún hospital coincide con la búsqueda.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FiltroTipo(texto: String, seleccionado: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = seleccionado,
        onClick = onClick,
        label = { Text(texto) },
        leadingIcon = if (seleccionado) {
            { Icon(IconosApio.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
        } else {
            null
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TarjetaHospital(
    hospital: Hospital,
    distanciaM: Double,
    esDestinoActivo: Boolean,
    etaS: Double?,
    onClick: () -> Unit,
) {
    val colorTipo = if (hospital.emergencia) {
        COLOR_POR_TIPO[hospital.tipo]?.let(::colorDesdeHex) ?: MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (esDestinoActivo) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).background(colorTipo, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(IconosApio.Hospital, contentDescription = null, tint = Color.White)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(hospital.nombre, style = MaterialTheme.typography.titleMedium)
                Text(
                    listOfNotNull(hospital.tipo, hospital.categoria, hospital.distrito).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!hospital.emergencia) {
                    Text(
                        "Sin emergencia · atención primaria",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                if (esDestinoActivo) {
                    Spacer(Modifier.height(6.dp))
                    Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primary) {
                        Text(
                            "Ruta activa",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    if (etaS != null) "${minutosCortos(etaS)} min" else formatearKmCorto(distanciaM),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    if (etaS != null) "con tráfico" else "en línea recta",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
