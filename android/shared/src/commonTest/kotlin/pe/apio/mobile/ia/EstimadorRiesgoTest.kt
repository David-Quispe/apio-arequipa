package pe.apio.mobile.ia

import pe.apio.mobile.modelo.AvenidaTrafico
import pe.apio.mobile.modelo.Hospital
import pe.apio.mobile.modelo.LatLon
import pe.apio.mobile.ui.EstadoRuta
import kotlin.test.Test
import kotlin.test.assertEquals

private val HOSPITAL_PRUEBA = Hospital(
    nombre = "Hospital de prueba",
    tipo = "MINSA",
    posicion = LatLon(-16.4, -71.5),
)

private fun ruta(
    tiempoS: Double,
    tiempoSConTrafico: Double,
    avenidasTraficoCruzadas: List<String> = emptyList(),
) = EstadoRuta(
    destino = HOSPITAL_PRUEBA,
    distanciaM = 1000.0,
    tiempoS = tiempoS,
    tiempoSConTrafico = tiempoSConTrafico,
    privilegiosCruzados = emptyList(),
    avenidasTraficoCruzadas = avenidasTraficoCruzadas,
    puntos = emptyList(),
)

class EstimadorRiesgoTest {

    @Test
    fun sin_demora_ni_trafico_el_riesgo_es_bajo() {
        val evaluacion = evaluarRiesgo(ruta(tiempoS = 300.0, tiempoSConTrafico = 300.0), trafico = emptyList())
        assertEquals(NivelRiesgo.BAJO, evaluacion.nivel)
    }

    @Test
    fun demora_fuerte_con_avenidas_en_trafico_alto_da_riesgo_alto() {
        val rutaConDemora = ruta(
            tiempoS = 300.0,
            tiempoSConTrafico = 600.0, // el doble de tiempo por trafico
            avenidasTraficoCruzadas = listOf("Av. Ejercito", "Av. Dolores"),
        )
        val trafico = listOf(
            AvenidaTrafico("Av. Ejercito", ratio = 0.4, nivel = "alta"),
            AvenidaTrafico("Av. Dolores", ratio = 0.3, nivel = "alta"),
        )
        val evaluacion = evaluarRiesgo(rutaConDemora, trafico)
        assertEquals(NivelRiesgo.ALTO, evaluacion.nivel)
    }

    @Test
    fun avenidas_en_trafico_bajo_no_suman_al_riesgo() {
        val rutaSinDemora = ruta(
            tiempoS = 300.0,
            tiempoSConTrafico = 300.0,
            avenidasTraficoCruzadas = listOf("Av. Ejercito"),
        )
        val trafico = listOf(AvenidaTrafico("Av. Ejercito", ratio = 0.9, nivel = "baja"))
        val evaluacion = evaluarRiesgo(rutaSinDemora, trafico)
        assertEquals(NivelRiesgo.BAJO, evaluacion.nivel)
    }
}
