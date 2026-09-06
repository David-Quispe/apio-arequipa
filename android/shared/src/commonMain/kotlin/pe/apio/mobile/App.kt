package pe.apio.mobile

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch
import pe.apio.mobile.mapa.MapaOSM
import pe.apio.mobile.modelo.HOSPITALES
import pe.apio.mobile.modelo.LatLon
import pe.apio.mobile.red.ApioApiClient

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
    var rutaPuntos by remember { mutableStateOf<List<LatLon>>(emptyList()) }

    MaterialTheme {
        MapaOSM(
            modifier = Modifier.fillMaxSize(),
            origen = origen,
            hospitales = HOSPITALES,
            rutaPuntos = rutaPuntos,
            onTapMapa = { nuevoOrigen ->
                origen = nuevoOrigen
                rutaPuntos = emptyList()
            },
            onTapHospital = { hospital ->
                scope.launch {
                    try {
                        val resultado = apiClient.obtenerRuta(
                            origenLat = origen.lat,
                            origenLon = origen.lon,
                            destLat = hospital.posicion.lat,
                            destLon = hospital.posicion.lon,
                        )
                        // GeoJSON viene como [lon, lat]; osmdroid necesita [lat, lon].
                        rutaPuntos = resultado.geometry.coordinates.map { LatLon(it[1], it[0]) }
                    } catch (e: Exception) {
                        // Manejo de error real (mensaje visible) llega en el
                        // paso 7 -- por ahora no se cae la app.
                        rutaPuntos = emptyList()
                    }
                }
            },
        )
    }
}
