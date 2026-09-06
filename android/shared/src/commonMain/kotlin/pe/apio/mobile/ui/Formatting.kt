package pe.apio.mobile.ui

import pe.apio.mobile.modelo.PrivilegioCruzado
import kotlin.math.round

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

/** Mismo formato que el panel de la PWA: "vía Av. X (Tipo N/10), ...". */
fun formatearPrivilegios(privilegios: List<PrivilegioCruzado>): String =
    privilegios.joinToString(", ") { p ->
        val tipo = p.tipoPrincipal ?: "Privilegio"
        val score = p.tipoPrincipalScore?.let { formatearDecimal(it, 1) } ?: "-"
        "${p.avenida} ($tipo $score/10)"
    }
