package pe.apio.mobile.mapa

import pe.apio.mobile.modelo.LatLon
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Una pieza del modelo 3D de la ambulancia: una figura sobre el suelo
 * ([contorno]) que el mapa levanta como un bloque desde [baseM] hasta
 * [altoM] metros, pintado de [color].
 */
data class PiezaAmbulancia(
    val contorno: List<LatLon>,
    val baseM: Double,
    val altoM: Double,
    val color: String,
)

// Medidas del modelo en metros (una ambulancia tipo furgon): x = hacia la
// derecha, y = hacia adelante, z = altura, con el origen en el centro.
private const val LARGO_MODELO_M = 6.0

// Pieza en coordenadas del modelo, antes de ubicarla en el mapa.
private class Molde(
    val puntos: List<Pair<Double, Double>>,
    val base: Double,
    val alto: Double,
    val color: String,
)

private const val BLANCO = "#F8FAFC"
private const val GRIS_TECHO = "#E2E8F0"
private const val ROJO = "#DC2626"
private const val VIDRIO = "#1E293B"
private const val LLANTA = "#111827"
private const val PARACHOQUES = "#475569"
private const val FARO = "#FDE68A"

// Barra de luces: cada mitad alterna entre encendida y apagada (destello)
private const val LUZ_IZQUIERDA = "luz-izquierda"
private const val LUZ_DERECHA = "luz-derecha"
private const val AZUL_ENCENDIDO = "#60A5FA"
private const val AZUL_APAGADO = "#1E3A8A"
private const val ROJO_ENCENDIDO = "#F87171"
private const val ROJO_APAGADO = "#7F1D1D"

// Tramos con los que se dibuja cada esquina redondeada
private const val PASOS_ESQUINA = 5

/**
 * Rectangulo con las esquinas redondeadas (radio [r]). El mapa solo sabe
 * levantar figuras hacia arriba, asi que las curvas del modelo salen de dos
 * trucos: contornos redondeados como este, y capas finas apiladas que se
 * van achicando (parabrisas inclinado, bordes del techo, ruedas).
 */
private fun redondeado(x1: Double, x2: Double, y1: Double, y2: Double, r: Double): List<Pair<Double, Double>> {
    val radio = min(r, min((x2 - x1) / 2, (y2 - y1) / 2))
    if (radio <= 0.0) return listOf(x1 to y1, x2 to y1, x2 to y2, x1 to y2)
    // centro de cada esquina, en el orden en que se recorre el contorno
    val centros = listOf(
        (x2 - radio) to (y2 - radio),
        (x1 + radio) to (y2 - radio),
        (x1 + radio) to (y1 + radio),
        (x2 - radio) to (y1 + radio),
    )
    val puntos = mutableListOf<Pair<Double, Double>>()
    centros.forEachIndexed { i, (cx, cy) ->
        for (k in 0..PASOS_ESQUINA) {
            val angulo = (i * 90.0 + 90.0 * k / PASOS_ESQUINA) * PI / 180
            puntos += (cx + radio * cos(angulo)) to (cy + radio * sin(angulo))
        }
    }
    return puntos
}

private fun bloque(
    x1: Double, x2: Double, y1: Double, y2: Double,
    base: Double, alto: Double, color: String, radio: Double = 0.0,
) = Molde(redondeado(x1, x2, y1, y2, radio), base, alto, color)

/** Una rueda redonda vista de costado: capas cuyo largo sigue el circulo. */
private fun rueda(xInterior: Double, xExterior: Double, yCentro: Double): List<Molde> {
    val radio = 0.5
    val capas = 7
    return (0 until capas).map { i ->
        val z1 = 2 * radio * i / capas
        val z2 = 2 * radio * (i + 1) / capas
        val alturaAlCentro = (z1 + z2) / 2 - radio
        val medioLargo = sqrt(radio * radio - alturaAlCentro * alturaAlCentro)
        bloque(
            min(xInterior, xExterior), maxOf(xInterior, xExterior),
            yCentro - medioLargo, yCentro + medioLargo,
            z1, z2, LLANTA,
        )
    }
}

private fun construirModelo(): List<Molde> {
    val piezas = mutableListOf<Molde>()

    // ruedas (asoman por los costados)
    for (y in listOf(1.9, -1.85)) {
        piezas += rueda(0.80, 1.20, y)
        piezas += rueda(-0.80, -1.20, y)
    }

    // parachoques: un zocalo gris apenas mas ancho que la carroceria
    piezas += bloque(-1.13, 1.13, -3.03, 3.03, 0.28, 0.55, PARACHOQUES, radio = 0.42)
    // carroceria baja, de punta a punta, con la trompa bien redondeada
    piezas += bloque(-1.10, 1.10, -3.00, 3.00, 0.55, 1.25, BLANCO, radio = 0.40)

    // capo: baja en pendiente hacia adelante (capas que se van acortando)
    for (i in 0 until 4) {
        piezas += bloque(-1.06, 1.06, 1.60, 2.95 - 0.16 * i, 1.25 + 0.06 * i, 1.25 + 0.06 * (i + 1), BLANCO, radio = 0.34)
    }

    // cabina: parabrisas inclinado y costados que se cierran hacia arriba
    val capasCabina = 6
    for (i in 0 until capasCabina) {
        val z1 = 1.49 + 0.66 * i / capasCabina
        val z2 = 1.49 + 0.66 * (i + 1) / capasCabina
        val frente = 2.25 - 0.50 * i / (capasCabina - 1)
        val medioAncho = 1.05 - 0.07 * i / (capasCabina - 1)
        piezas += bloque(-medioAncho, medioAncho, 0.95, frente, z1, z2, VIDRIO, radio = 0.22)
    }
    // techo de la cabina, con el borde suavizado
    piezas += bloque(-0.99, 0.99, 0.95, 1.76, 2.15, 2.24, BLANCO, radio = 0.22)
    piezas += bloque(-0.90, 0.90, 0.95, 1.68, 2.24, 2.30, GRIS_TECHO, radio = 0.22)

    // modulo sanitario: franja roja abajo y caja blanca de esquinas redondeadas
    piezas += bloque(-1.13, 1.13, -3.03, 1.03, 1.25, 1.50, ROJO, radio = 0.30)
    piezas += bloque(-1.10, 1.10, -3.00, 1.00, 1.50, 2.58, BLANCO, radio = 0.28)
    // el techo se va cerrando: borde curvo en vez de arista viva
    piezas += bloque(-1.06, 1.06, -2.96, 0.96, 2.58, 2.67, BLANCO, radio = 0.28)
    piezas += bloque(-0.98, 0.98, -2.88, 0.88, 2.67, 2.74, GRIS_TECHO, radio = 0.28)

    // barra de luces sobre el frente de la caja: azul y roja, redondeadas
    piezas += bloque(-0.82, -0.04, 0.36, 0.76, 2.74, 2.94, LUZ_IZQUIERDA, radio = 0.14)
    piezas += bloque(0.04, 0.82, 0.36, 0.76, 2.74, 2.94, LUZ_DERECHA, radio = 0.14)
    piezas += bloque(-0.74, -0.12, 0.43, 0.69, 2.94, 3.00, LUZ_IZQUIERDA, radio = 0.12)
    piezas += bloque(0.12, 0.74, 0.43, 0.69, 2.94, 3.00, LUZ_DERECHA, radio = 0.12)

    // cruz roja en el techo
    piezas += bloque(-0.20, 0.20, -2.40, -0.70, 2.74, 2.78, ROJO, radio = 0.06)
    piezas += bloque(-0.76, 0.76, -1.75, -1.35, 2.74, 2.78, ROJO, radio = 0.06)

    // faros adelante y luces rojas atras
    piezas += bloque(-0.72, -0.38, 2.94, 3.03, 0.80, 1.08, FARO)
    piezas += bloque(0.38, 0.72, 2.94, 3.03, 0.80, 1.08, FARO)
    piezas += bloque(-0.80, -0.48, -3.04, -2.96, 1.60, 1.95, ROJO)
    piezas += bloque(0.48, 0.80, -3.04, -2.96, 1.60, 1.95, ROJO)

    return piezas
}

// El modelo se arma una sola vez; despues solo se ubica y se gira.
private val MOLDES: List<Molde> = construirModelo()

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

    // punto local (x, y) en metros -> coordenada del mapa, ya girada
    fun enMapa(x: Double, y: Double): LatLon {
        val este = (x * coseno + y * seno) * escala
        val norte = (-x * seno + y * coseno) * escala
        return LatLon(centro.lat + norte / 110_540.0, centro.lon + este / metrosPorGradoLon)
    }

    return MOLDES.map { molde ->
        PiezaAmbulancia(
            contorno = molde.puntos.map { (x, y) -> enMapa(x, y) },
            baseM = molde.base * escala,
            altoM = molde.alto * escala,
            color = when (molde.color) {
                LUZ_IZQUIERDA -> if (destello) AZUL_APAGADO else AZUL_ENCENDIDO
                LUZ_DERECHA -> if (destello) ROJO_ENCENDIDO else ROJO_APAGADO
                else -> molde.color
            },
        )
    }
}
