package pe.apio.mobile.mapa

import pe.apio.mobile.modelo.LatLon
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * Una pieza del modelo 3D de la ambulancia: un rectangulo sobre el suelo
 * ([contorno], 4 esquinas) que el mapa levanta como un bloque desde
 * [baseM] hasta [altoM] metros, pintado de [color].
 */
data class PiezaAmbulancia(
    val contorno: List<LatLon>,
    val baseM: Double,
    val altoM: Double,
    val color: String,
)

// Medidas del modelo en metros (una ambulancia tipo furgon): x = hacia la
// derecha, y = hacia adelante, con el origen en el centro del vehiculo.
private const val LARGO_MODELO_M = 6.0

private class Bloque(
    val x1: Double, val x2: Double,
    val y1: Double, val y2: Double,
    val base: Double, val alto: Double,
    val color: String,
)

private const val BLANCO = "#F8FAFC"
private const val ROJO = "#DC2626"
private const val VIDRIO = "#1E293B"
private const val LLANTA = "#111827"
// Barra de luces: cada mitad alterna entre encendida y apagada (destello)
private const val LUZ_IZQUIERDA = "luz-izquierda"
private const val LUZ_DERECHA = "luz-derecha"
private const val AZUL_ENCENDIDO = "#60A5FA"
private const val AZUL_APAGADO = "#1E3A8A"
private const val ROJO_ENCENDIDO = "#F87171"
private const val ROJO_APAGADO = "#7F1D1D"

// La ambulancia armada con bloques, de abajo hacia arriba. Las piezas de
// color sobresalen unos centimetros de la carroceria para que no se
// "peleen" dos caras en el mismo plano al dibujarse.
private val BLOQUES = listOf(
    // llantas (asoman por los costados)
    Bloque(-1.22, -0.80, 1.45, 2.35, 0.0, 0.85, LLANTA),
    Bloque(0.80, 1.22, 1.45, 2.35, 0.0, 0.85, LLANTA),
    Bloque(-1.22, -0.80, -2.25, -1.35, 0.0, 0.85, LLANTA),
    Bloque(0.80, 1.22, -2.25, -1.35, 0.0, 0.85, LLANTA),
    // carroceria baja, de punta a punta (incluye el capo)
    Bloque(-1.10, 1.10, -3.00, 3.00, 0.35, 1.25, BLANCO),
    // franja roja alrededor del modulo sanitario
    Bloque(-1.13, 1.13, -3.03, 1.00, 1.25, 1.50, ROJO),
    // modulo sanitario (la caja de atras)
    Bloque(-1.10, 1.10, -3.00, 1.00, 1.50, 2.75, BLANCO),
    // cabina: vidrios oscuros y techo
    Bloque(-1.06, 1.06, 1.00, 2.25, 1.25, 2.10, VIDRIO),
    Bloque(-1.08, 1.08, 1.00, 2.20, 2.10, 2.28, BLANCO),
    // barra de luces sobre el frente de la caja: azul y roja
    Bloque(-0.85, -0.05, 0.45, 0.85, 2.75, 3.02, LUZ_IZQUIERDA),
    Bloque(0.05, 0.85, 0.45, 0.85, 2.75, 3.02, LUZ_DERECHA),
    // cruz roja en el techo
    Bloque(-0.22, 0.22, -2.45, -0.65, 2.75, 2.80, ROJO),
    Bloque(-0.80, 0.80, -1.77, -1.33, 2.75, 2.80, ROJO),
    // luces traseras
    Bloque(-1.05, -0.70, -3.06, -3.00, 1.55, 1.90, ROJO),
    Bloque(0.70, 1.05, -3.06, -3.00, 1.55, 1.90, ROJO),
)

/**
 * Metros que ocupa un pixel del mapa a ese zoom y latitud (teselas de 512 px,
 * las que usa MapLibre).
 */
fun metrosPorPixel(latitud: Double, zoom: Double): Double =
    78271.517 * cos(latitud * PI / 180) / 2.0.pow(zoom)

/**
 * Arma el modelo 3D de la ambulancia ubicado en [centro] y apuntando hacia
 * [rumbo] (grados, 0 = norte). [largoM] es el largo que debe medir sobre el
 * mapa: como una ambulancia real (6 m) seria un punto a zoom de ciudad, el
 * mapa la pide mas grande para que siempre se vea de un tamano comodo; todo
 * el modelo (ancho y alturas) se escala en la misma proporcion.
 *
 * [destello] elige cual mitad de la barra de luces esta encendida: el mapa
 * lo va alternando para que las luces parpadeen (azul / rojo).
 */
fun modeloAmbulancia(
    centro: LatLon,
    rumbo: Float,
    largoM: Double,
    destello: Boolean = false,
): List<PiezaAmbulancia> {
    val escala = largoM / LARGO_MODELO_M
    val giro = rumbo * PI / 180
    val seno = sin(giro)
    val coseno = cos(giro)
    val metrosPorGradoLon = 111_320.0 * cos(centro.lat * PI / 180)

    // esquina local (x, y) en metros -> coordenada del mapa, ya girada
    fun esquina(x: Double, y: Double): LatLon {
        val este = (x * coseno + y * seno) * escala
        val norte = (-x * seno + y * coseno) * escala
        return LatLon(centro.lat + norte / 110_540.0, centro.lon + este / metrosPorGradoLon)
    }

    return BLOQUES.map { b ->
        PiezaAmbulancia(
            contorno = listOf(esquina(b.x1, b.y1), esquina(b.x2, b.y1), esquina(b.x2, b.y2), esquina(b.x1, b.y2)),
            baseM = b.base * escala,
            altoM = b.alto * escala,
            color = when (b.color) {
                LUZ_IZQUIERDA -> if (destello) AZUL_APAGADO else AZUL_ENCENDIDO
                LUZ_DERECHA -> if (destello) ROJO_ENCENDIDO else ROJO_APAGADO
                else -> b.color
            },
        )
    }
}
