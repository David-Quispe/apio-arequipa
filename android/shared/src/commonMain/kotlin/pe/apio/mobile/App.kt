package pe.apio.mobile

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import pe.apio.mobile.mapa.MapaOSM
import pe.apio.mobile.modelo.LatLon

// Centro del corredor piloto (mismo punto que frontend/src/App.jsx).
private val CENTRO_AREQUIPA = LatLon(-16.3989, -71.5369)

@Composable
@Preview
fun App() {
    MaterialTheme {
        MapaOSM(
            modifier = Modifier.fillMaxSize(),
            origen = CENTRO_AREQUIPA,
        )
    }
}
