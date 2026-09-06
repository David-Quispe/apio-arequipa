package pe.apio.mobile

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
    MaterialTheme {
        MapaOSM(
            modifier = Modifier.fillMaxSize(),
            origen = ORIGEN_INICIAL,
            hospitales = HOSPITALES,
        )
    }
}
