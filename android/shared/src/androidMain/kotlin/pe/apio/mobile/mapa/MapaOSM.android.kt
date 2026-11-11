package pe.apio.mobile.mapa

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import pe.apio.mobile.modelo.COLOR_AMBULANCIA
import pe.apio.mobile.modelo.COLOR_POR_TIPO
import pe.apio.mobile.modelo.Hospital
import pe.apio.mobile.modelo.LatLon
import java.io.File

private const val COLOR_TIPO_DESCONOCIDO = "#6b7280"
// Establecimientos sin emergencia (puestos y centros de salud I-1 a I-4):
// gris y mas chicos, para que no compitan con los destinos de ambulancia.
private const val COLOR_SIN_EMERGENCIA = "#9ca3af"

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
    // Referencias a los overlays para actualizarlos en `update` sin recrear
    // el MapView (y sus tiles) en cada recomposicion -- el punto delicado de
    // mezclar el modelo imperativo de osmdroid con Compose.
    val marcadorOrigenRef = remember { arrayOfNulls<Marker>(1) }
    val polylineRutaRef = remember { arrayOfNulls<Polyline>(1) }
    val marcadoresHospital = remember { mutableListOf<Marker>() }
    val hospitalesDibujados = remember { arrayOfNulls<List<Hospital>>(1) }
    // Un mismo icono se comparte entre todos los marcadores de igual color y
    // tamano (con cientos de establecimientos no conviene un bitmap por cada uno).
    val iconos = remember { mutableMapOf<String, Drawable>() }

    val tapMapa by rememberUpdatedState(onTapMapa)
    val tapHospital by rememberUpdatedState(onTapHospital)

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            configurarOsmdroid(ctx)
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(ZOOM_INICIAL)
                controller.setCenter(GeoPoint(origen.lat, origen.lon))

                // Va primero: osmdroid revisa los overlays en orden inverso al
                // tocar, asi que un marcador "tapa" a este catch-all y solo se
                // dispara cuando el toque no cayo sobre ningun marcador.
                overlays.add(
                    MapEventsOverlay(object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                            tapMapa(LatLon(p.latitude, p.longitude))
                            return true
                        }

                        override fun longPressHelper(p: GeoPoint): Boolean = false
                    })
                )

                val polylineRuta = Polyline(this).apply {
                    // azul primario del tema (Figma), igual que la ruta del diseno
                    outlinePaint.color = android.graphics.Color.parseColor("#004790")
                    outlinePaint.strokeWidth = 5f * resources.displayMetrics.density
                }
                overlays.add(polylineRuta)
                polylineRutaRef[0] = polylineRuta

                val marcadorOrigen = Marker(this)
                marcadorOrigen.position = GeoPoint(origen.lat, origen.lon)
                marcadorOrigen.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                marcadorOrigen.icon = crearIconoCircular(ctx, emoji = "🚑", colorHex = COLOR_AMBULANCIA, sizeDp = 38)
                overlays.add(marcadorOrigen)
                marcadorOrigenRef[0] = marcadorOrigen
            }
        },
        update = { mapView ->
            // La lista de establecimientos llega del backend despues de abrir
            // la app (y cambia con el filtro "Con emergencia / Todos"): solo
            // se rehacen los marcadores cuando la lista cambio de verdad.
            if (hospitalesDibujados[0] != hospitales) {
                mapView.overlays.removeAll(marcadoresHospital.toSet())
                marcadoresHospital.clear()
                hospitales.forEach { hospital ->
                    val color = if (hospital.emergencia) {
                        COLOR_POR_TIPO[hospital.tipo] ?: COLOR_TIPO_DESCONOCIDO
                    } else {
                        COLOR_SIN_EMERGENCIA
                    }
                    val tamano = if (hospital.emergencia) 34 else 22
                    val icono = iconos.getOrPut("$color-$tamano") {
                        crearIconoCircular(mapView.context, emoji = "🏥", colorHex = color, sizeDp = tamano)
                    }
                    val marcador = Marker(mapView).apply {
                        position = GeoPoint(hospital.posicion.lat, hospital.posicion.lon)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        this.icon = icono
                        title = hospital.nombre
                        snippet = listOfNotNull(hospital.categoria, hospital.tipo).joinToString(" · ")
                        setOnMarkerClickListener { _, _ ->
                            tapHospital(hospital)
                            true
                        }
                    }
                    marcadoresHospital += marcador
                }
                // justo despues del catch-all de toques (indice 0), y debajo de
                // la ruta y de la ambulancia para que esas queden siempre encima
                mapView.overlays.addAll(1.coerceAtMost(mapView.overlays.size), marcadoresHospital)
                hospitalesDibujados[0] = hospitales
            }

            marcadorOrigenRef[0]?.position = GeoPoint(origen.lat, origen.lon)
            polylineRutaRef[0]?.setPoints(rutaPuntos.map { GeoPoint(it.lat, it.lon) })
            mapView.invalidate()
        },
    )
}
