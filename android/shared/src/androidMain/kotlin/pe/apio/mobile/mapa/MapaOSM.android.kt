package pe.apio.mobile.mapa

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import pe.apio.mobile.modelo.Hospital
import pe.apio.mobile.modelo.LatLon
import java.io.File

private const val ZOOM_INICIAL = 13.0

// osmdroid exige un User-Agent propio o el servidor de tiles de OSM responde
// 403; se guarda la cache/config en almacenamiento propio de la app para no
// tener que pedir permisos de almacenamiento externo.
private fun configurarOsmdroid(context: Context) {
    val base = context.getExternalFilesDir(null) ?: context.filesDir
    Configuration.getInstance().apply {
        userAgentValue = context.packageName
        osmdroidBasePath = base
        osmdroidTileCache = File(base, "tiles")
    }
}

@Composable
actual fun MapaOSM(
    modifier: Modifier,
    origen: LatLon,
    hospitales: List<Hospital>,
    rutaPuntos: List<LatLon>,
    onTapMapa: (LatLon) -> Unit,
    onTapHospital: (Hospital) -> Unit,
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            configurarOsmdroid(ctx)
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(ZOOM_INICIAL)
                controller.setCenter(GeoPoint(origen.lat, origen.lon))
            }
        },
    )
}
