package pe.apio.mobile.mapa

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import pe.apio.mobile.modelo.Hospital
import pe.apio.mobile.modelo.LatLon

/**
 * Mapa de la app. Unica pieza especifica de plataforma (MapLibre Native es
 * Android-only) -- todo lo demas (modelos, red, estado) vive en commonMain.
 * Si se agrega iOS despues, solo hace falta un MapaApio.ios.kt nuevo, sin
 * tocar nada de este contrato.
 *
 * Tiene dos formas de mostrarse:
 *  - planificacion ([modoNavegacion] = false): camara plana, norte arriba;
 *    al calcular una ruta se encuadra completa.
 *  - navegacion ([modoNavegacion] = true): camara inclinada que sigue a la
 *    ambulancia y gira segun su [rumbo], como Google Maps. Si el usuario
 *    arrastra el mapa se avisa con [onCamaraMovidaPorUsuario]; la pantalla
 *    pone [siguiendoCamara] en false y ofrece "Recentrar".
 *
 * [rumbo] va en grados (0 = norte, 90 = este). [margenInferiorPx] es el alto
 * de lo que tapa la parte de abajo del mapa (la hoja de ruta), para que la
 * camara no deje la ambulancia ni la ruta escondidas detras. [oscuro] elige
 * el estilo nocturno del mapa.
 */
@Composable
expect fun MapaApio(
    modifier: Modifier = Modifier,
    origen: LatLon,
    rumbo: Float = 0f,
    hospitales: List<Hospital> = emptyList(),
    rutaPuntos: List<LatLon> = emptyList(),
    modoNavegacion: Boolean = false,
    siguiendoCamara: Boolean = true,
    oscuro: Boolean = false,
    margenInferiorPx: Int = 0,
    onTapMapa: (LatLon) -> Unit = {},
    onTapHospital: (Hospital) -> Unit = {},
    onCamaraMovidaPorUsuario: () -> Unit = {},
)
