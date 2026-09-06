package pe.apio.mobile.mapa

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import pe.apio.mobile.modelo.COLOR_AMBULANCIA
import pe.apio.mobile.modelo.COLOR_POR_TIPO
import pe.apio.mobile.modelo.Hospital
import pe.apio.mobile.modelo.LatLon
import java.io.File

private const val COLOR_TIPO_DESCONOCIDO = "#6b7280"

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

                hospitales.forEach { hospital ->
                    val marcador = Marker(this)
                    marcador.position = GeoPoint(hospital.posicion.lat, hospital.posicion.lon)
                    marcador.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    marcador.icon = crearIconoCircular(
                        ctx,
                        emoji = "🏥", // 🏥
                        colorHex = COLOR_POR_TIPO[hospital.tipo] ?: COLOR_TIPO_DESCONOCIDO,
                    )
                    marcador.title = hospital.nombre
                    marcador.snippet = hospital.tipo
                    overlays.add(marcador)
                }

                val marcadorOrigen = Marker(this)
                marcadorOrigen.position = GeoPoint(origen.lat, origen.lon)
                marcadorOrigen.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                marcadorOrigen.icon = crearIconoCircular(
                    ctx,
                    emoji = "🚑", // 🚑
                    colorHex = COLOR_AMBULANCIA,
                    sizeDp = 38,
                )
                overlays.add(marcadorOrigen)
            }
        },
    )
}
