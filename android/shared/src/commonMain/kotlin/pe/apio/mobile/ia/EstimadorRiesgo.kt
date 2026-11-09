package pe.apio.mobile.ia

import pe.apio.mobile.modelo.AvenidaTrafico
import pe.apio.mobile.ui.EstadoRuta

enum class NivelRiesgo { BAJO, MEDIO, ALTO }

data class EvaluacionRiesgo(
    val nivel: NivelRiesgo,
    val mensaje: String,
    val puntaje: Double,
)

/**
 * Heuristica propia de la app -- la "IA" del criterio de la rubrica. No es
 * un modelo entrenado con un dataset externo: es un arbol de decision
 * ponderado, disenado a mano con el conocimiento de dominio del corredor,
 * que combina 3 señales reales que ya calcula el backend a partir de
 * trafico real de TomTom (ver backend/app/services/traffic.py):
 *
 *  - cuanto se alarga el ETA por trafico (peso 0.5): la señal mas directa.
 *  - cuantas de las avenidas que cruza la ruta estan en nivel "alta" de
 *    trafico (peso 0.3).
 *  - que tan exigentes son los privilegios cruzados que la ambulancia
 *    necesita usar -- contraflujo, carril exclusivo, cruce en rojo -- (peso
 *    0.2): a mas privilegio exigido, mas maniobras de riesgo en el trayecto.
 *
 * El resultado es un puntaje 0..1 clasificado en 3 niveles, explicable
 * punto por punto (no es una caja negra), que el panel de info muestra como
 * recomendacion para quien conduce la ambulancia.
 */
fun evaluarRiesgo(ruta: EstadoRuta, trafico: List<AvenidaTrafico>): EvaluacionRiesgo {
    val demoraRatio = if (ruta.tiempoS > 0) {
        ((ruta.tiempoSConTrafico - ruta.tiempoS) / ruta.tiempoS).coerceIn(0.0, 1.0)
    } else {
        0.0
    }

    val nivelesPorAvenida = trafico.associate { it.avenida to it.nivel }
    val avenidasAltaRatio = if (ruta.avenidasTraficoCruzadas.isNotEmpty()) {
        val enAlta = ruta.avenidasTraficoCruzadas.count { nivelesPorAvenida[it] == "alta" }
        enAlta.toDouble() / ruta.avenidasTraficoCruzadas.size
    } else {
        0.0
    }

    val privilegioRatio = if (ruta.privilegiosCruzados.isNotEmpty()) {
        val promedio = ruta.privilegiosCruzados.map { it.general }.average()
        (promedio / 10.0).coerceIn(0.0, 1.0)
    } else {
        0.0
    }

    val puntaje = (0.5 * demoraRatio + 0.3 * avenidasAltaRatio + 0.2 * privilegioRatio)
        .coerceIn(0.0, 1.0)

    val nivel = when {
        puntaje < 0.33 -> NivelRiesgo.BAJO
        puntaje < 0.66 -> NivelRiesgo.MEDIO
        else -> NivelRiesgo.ALTO
    }

    val mensaje = when (nivel) {
        NivelRiesgo.BAJO -> "Trafico favorable: el ETA se mantiene cercano al tiempo sin trafico."
        NivelRiesgo.MEDIO -> "Demora moderada por trafico real: reconfirma la ruta antes de salir."
        NivelRiesgo.ALTO -> "Alta probabilidad de demora: el trafico esta afectando fuerte esta ruta."
    }

    return EvaluacionRiesgo(nivel, mensaje, puntaje)
}
