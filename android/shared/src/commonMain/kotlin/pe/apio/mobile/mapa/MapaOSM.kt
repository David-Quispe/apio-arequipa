package pe.apio.mobile.mapa

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import pe.apio.mobile.modelo.Hospital
import pe.apio.mobile.modelo.LatLon

/**
 * Mapa del corredor piloto. Unica pieza especifica de plataforma de toda la
 * app (osmdroid es Android-only) -- todo lo demas (modelos, red, estado)
 * vive en commonMain. Si se agrega iOS despues, solo hace falta un
 * MapaOSM.ios.kt nuevo (ej. con MapKit), sin tocar nada de este contrato.
 */
@Composable
expect fun MapaOSM(
    modifier: Modifier = Modifier,
    origen: LatLon,
    hospitales: List<Hospital> = emptyList(),
    rutaPuntos: List<LatLon> = emptyList(),
    onTapMapa: (LatLon) -> Unit = {},
    onTapHospital: (Hospital) -> Unit = {},
)
