package pe.apio.mobile

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import pe.apio.mobile.mapa.MapaOSM
import pe.apio.mobile.modelo.HOSPITALES
import pe.apio.mobile.modelo.LatLon

// Cerro Colorado, zona de origen del corredor (mismo punto que
// ORIGEN_INICIAL en frontend/src/App.jsx).
private val ORIGEN_INICIAL = LatLon(-16.3833, -71.55)

@Composable
@Preview
fun App() {
    var origen by remember { mutableStateOf(ORIGEN_INICIAL) }

    MaterialTheme {
        MapaOSM(
            modifier = Modifier.fillMaxSize(),
            origen = origen,
            hospitales = HOSPITALES,
            onTapMapa = { origen = it },
        )
    }
}
