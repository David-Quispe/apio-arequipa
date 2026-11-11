package pe.apio.mobile.mapa

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.maps.MapView
import pe.apio.mobile.modelo.Hospital
import pe.apio.mobile.modelo.LatLon

/**
 * Implementacion Android del mapa con MapLibre Native (mapa vectorial: se
 * puede inclinar y rotar, a diferencia de osmdroid que era solo 2D).
 *
 * Este composable se ocupa de las tres cosas que MapLibre no resuelve solo
 * dentro de Compose: crear el MapView una unica vez, avisarle del ciclo de
 * vida de la pantalla y pasarle el estado nuevo. Todo el dibujo esta en
 * [ControladorMapa].
 */
@Composable
actual fun MapaApio(
    modifier: Modifier,
    origen: LatLon,
    rumbo: Float,
    hospitales: List<Hospital>,
    rutaPuntos: List<LatLon>,
    modoNavegacion: Boolean,
    siguiendoCamara: Boolean,
    oscuro: Boolean,
    margenInferiorPx: Int,
    onTapMapa: (LatLon) -> Unit,
    onTapHospital: (Hospital) -> Unit,
    onCamaraMovidaPorUsuario: () -> Unit,
) {
    val contexto = LocalContext.current
    val cicloDeVida = LocalLifecycleOwner.current.lifecycle

    // Las lambdas cambian en cada recomposicion; el controlador se queda con
    // estas referencias, que siempre apuntan a la version mas nueva.
    val tapMapa by rememberUpdatedState(onTapMapa)
    val tapHospital by rememberUpdatedState(onTapHospital)
    val camaraMovida by rememberUpdatedState(onCamaraMovidaPorUsuario)

    val mapView = remember {
        // MapLibre exige inicializarse antes de crear cualquier MapView
        MapLibre.getInstance(contexto.applicationContext)
        MapView(contexto).apply { onCreate(null) }
    }
    val controlador = remember(mapView) {
        ControladorMapa(mapView, origenInicial = origen, rumboInicial = rumbo, oscuro = oscuro).apply {
            alTocarMapa = { punto -> tapMapa(punto) }
            alTocarHospital = { hospital -> tapHospital(hospital) }
            alMoverCamaraConElDedo = { camaraMovida() }
        }
    }

    // El MapView es una vista clasica con su propio motor de render: hay que
    // reenviarle start/resume/pause/stop o sigue dibujando (y gastando
    // bateria) con la app minimizada, y destruirlo al salir de la pantalla.
    DisposableEffect(cicloDeVida, mapView) {
        var iniciado = false
        var reanudado = false
        val observador = LifecycleEventObserver { _, evento ->
            when (evento) {
                Lifecycle.Event.ON_START -> {
                    mapView.onStart()
                    iniciado = true
                }
                Lifecycle.Event.ON_RESUME -> {
                    mapView.onResume()
                    reanudado = true
                }
                Lifecycle.Event.ON_PAUSE -> {
                    mapView.onPause()
                    reanudado = false
                }
                Lifecycle.Event.ON_STOP -> {
                    mapView.onStop()
                    iniciado = false
                }
                else -> Unit
            }
        }
        // al registrarse, el observador recibe de inmediato los eventos que
        // ya pasaron (start y resume si la pantalla esta visible)
        cicloDeVida.addObserver(observador)
        onDispose {
            cicloDeVida.removeObserver(observador)
            controlador.destruir()
            // cierre en orden aunque el composable salga con la app en primer
            // plano (por ejemplo al cambiar de pestana)
            if (reanudado) mapView.onPause()
            if (iniciado) mapView.onStop()
            mapView.onDestroy()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { mapView },
        update = {
            controlador.actualizar(
                origen = origen,
                rumbo = rumbo,
                hospitales = hospitales,
                ruta = rutaPuntos,
                modoNavegacion = modoNavegacion,
                siguiendoCamara = siguiendoCamara,
                oscuro = oscuro,
                margenInferiorPx = margenInferiorPx,
            )
        },
    )
}
