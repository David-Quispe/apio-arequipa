package pe.apio.mobile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pe.apio.mobile.mapa.MapaOSM
import pe.apio.mobile.modelo.HOSPITALES
import pe.apio.mobile.modelo.LatLon
import pe.apio.mobile.red.ApioApiClient
import pe.apio.mobile.ui.EstadoRuta
import pe.apio.mobile.ui.PanelInfo

// Cerro Colorado, zona de origen del corredor (mismo punto que
// ORIGEN_INICIAL en frontend/src/App.jsx).
private val ORIGEN_INICIAL = LatLon(-16.3833, -71.55)

// 10.0.2.2 es el alias del emulador Android hacia el localhost de la
// maquina host, donde corre el backend real (ver AndroidManifest.xml /
// network_security_config.xml para el permiso de trafico sin cifrar).
private const val API_BASE_URL = "http://10.0.2.2:8000/api"

@Composable
@Preview
fun App() {
    val apiClient = remember { ApioApiClient(API_BASE_URL) }
    val scope = rememberCoroutineScope()

    var origen by remember { mutableStateOf(ORIGEN_INICIAL) }
    var ruta by remember { mutableStateOf<EstadoRuta?>(null) }
    var cargando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            MapaOSM(
                modifier = Modifier.fillMaxSize(),
                origen = origen,
                hospitales = HOSPITALES,
                rutaPuntos = ruta?.puntos ?: emptyList(),
                onTapMapa = { nuevoOrigen ->
                    origen = nuevoOrigen
                    ruta = null
                    error = null
                },
                onTapHospital = { hospital ->
                    cargando = true
                    error = null
                    scope.launch {
                        try {
                            val resultado = apiClient.obtenerRuta(
                                origenLat = origen.lat,
                                origenLon = origen.lon,
                                destLat = hospital.posicion.lat,
                                destLon = hospital.posicion.lon,
                            )
                            // GeoJSON viene como [lon, lat]; osmdroid necesita [lat, lon].
                            val puntos = resultado.geometry.coordinates.map { LatLon(it[1], it[0]) }
                            ruta = EstadoRuta(
                                destinoNombre = hospital.nombre,
                                distanciaM = resultado.distanceM,
                                tiempoS = resultado.timeS,
                                tiempoSConTrafico = resultado.timeSConTrafico,
                                privilegiosCruzados = resultado.privilegiosCruzados,
                                puntos = puntos,
                            )
                        } catch (e: Exception) {
                            error = e.message ?: "No se pudo calcular la ruta"
                            ruta = null
                        } finally {
                            cargando = false
                        }
                    }
                },
            )

            PanelInfo(
                cargando = cargando,
                error = error,
                ruta = ruta,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(12.dp)
                    .fillMaxWidth(),
            )
        }
    }
}
