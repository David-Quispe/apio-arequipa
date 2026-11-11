package pe.apio.mobile.ui

import pe.apio.mobile.modelo.LatLon
import pe.apio.mobile.modelo.PrivilegioCruzado
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt

// Formato con decimales fijos sin depender de String.format/java.lang.Math
// (no existen en commonMain) -- asi esto sigue siendo portable si se agrega
// iOS despues.
internal fun formatearDecimal(valor: Double, decimales: Int): String {
    var factor = 1.0
    repeat(decimales) { factor *= 10.0 }
    val redondeado = round(valor * factor) / factor
    if (decimales <= 0) return round(redondeado).toLong().toString()
    val parteEntera = redondeado.toLong()
    val parteDecimal = round((redondeado - parteEntera) * factor).toLong()
    return "$parteEntera.${parteDecimal.toString().padStart(decimales, '0')}"
}

fun formatearDistanciaKm(distanciaM: Double): String =
    "${formatearDecimal(distanciaM / 1000, 2)} km"

fun formatearMinutos(segundos: Double): String =
    "${formatearDecimal(segundos / 60, 1)} min"

/** Solo el numero de minutos con 1 decimal, para mostrarlo grande ("7.4"). */
fun minutosCortos(segundos: Double): String = formatearDecimal(segundos / 60, 1)

fun formatearKmCorto(distanciaM: Double): String = "${formatearDecimal(distanciaM / 1000, 1)} km"

// ---- Fechas del historial ----
// Peru no tiene horario de verano: UTC-5 fijo todo el anio, asi que no hace
// falta una libreria de fechas para mostrar la hora local.
private const val OFFSET_LIMA_MS = -5L * 60 * 60 * 1000
private const val MS_POR_DIA = 24L * 60 * 60 * 1000

/** Numero de dia (en hora de Lima) para agrupar traslados en Hoy / Ayer. */
internal fun diaLocal(epochMs: Long): Long = (epochMs + OFFSET_LIMA_MS).floorDiv(MS_POR_DIA)

/** Hora local "HH:mm" de un timestamp. */
internal fun horaLocal(epochMs: Long): String {
    val msDelDia = (epochMs + OFFSET_LIMA_MS).mod(MS_POR_DIA)
    val horas = msDelDia / 3_600_000
    val minutos = (msDelDia / 60_000) % 60
    return "${horas.toString().padStart(2, '0')}:${minutos.toString().padStart(2, '0')}"
}

internal fun etiquetaDia(dia: Long, hoy: Long): String = when (val diferencia = hoy - dia) {
    0L -> "Hoy"
    1L -> "Ayer"
    else -> "Hace $diferencia días"
}

// ---- Distancia en linea recta ----

/** Distancia haversine en metros: sirve para ordenar hospitales por cercania
 * en la lista sin pedir una ruta al backend por cada uno. */
internal fun distanciaLineaRectaM(a: LatLon, b: LatLon): Double {
    val radioTierraM = 6_371_000.0
    val aRad = { grados: Double -> grados * PI / 180.0 }
    val dLat = aRad(b.lat - a.lat)
    val dLon = aRad(b.lon - a.lon)
    val h = sin(dLat / 2) * sin(dLat / 2) +
        cos(aRad(a.lat)) * cos(aRad(b.lat)) * sin(dLon / 2) * sin(dLon / 2)
    return 2 * radioTierraM * asin(sqrt(h))
}

/** Mismo formato que el panel de la PWA: "vía Av. X (Tipo N/10), ...". */
fun formatearPrivilegios(privilegios: List<PrivilegioCruzado>): String =
    privilegios.joinToString(", ") { p ->
        val tipo = p.tipoPrincipal ?: "Privilegio"
        val score = p.tipoPrincipalScore?.let { formatearDecimal(it, 1) } ?: "-"
        "${p.avenida} ($tipo $score/10)"
    }
