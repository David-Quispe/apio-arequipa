package pe.apio.mobile.mapa

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.MapEventsOverlay
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
    // Referencia al marcador de origen para poder moverlo en `update` sin
    // recrear todo el MapView (y sus overlays/tiles) en cada recomposicion
    // -- el punto delicado de mezclar el modelo imperativo de osmdroid con
    // la recomposicion declarativa de Compose.
    val marcadorOrigenRef = remember { arrayOfNulls<Marker>(1) }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            configurarOsmdroid(ctx)
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(ZOOM_INICIAL)
                controller.setCenter(GeoPoint(origen.lat, origen.lon))

                // Se agrega antes que los marcadores: osmdroid revisa los
                // overlays en orden inverso al tocar la pantalla, asi que
                // un marcador "tapa" a este catch-all y solo se dispara
                // cuando el toque no cayo sobre ningun marcador.
                overlays.add(
                    MapEventsOverlay(object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                            onTapMapa(LatLon(p.latitude, p.longitude))
                            return true
                        }

                        override fun longPressHelper(p: GeoPoint): Boolean = false
                    })
                )

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
                marcadorOrigenRef[0] = marcadorOrigen
            }
        },
        update = { mapView ->
            marcadorOrigenRef[0]?.position = GeoPoint(origen.lat, origen.lon)
            mapView.invalidate()
        },
    )
}
