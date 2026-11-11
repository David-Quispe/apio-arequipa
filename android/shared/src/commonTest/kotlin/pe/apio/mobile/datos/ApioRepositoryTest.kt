package pe.apio.mobile.datos

import pe.apio.mobile.ia.EvaluacionRiesgo
import pe.apio.mobile.ia.NivelRiesgo
import pe.apio.mobile.modelo.AvenidaTrafico
import pe.apio.mobile.modelo.Especificos
import pe.apio.mobile.modelo.EstablecimientoDto
import pe.apio.mobile.modelo.Hospital
import pe.apio.mobile.modelo.LatLon
import pe.apio.mobile.modelo.PrivilegioCruzado
import pe.apio.mobile.ui.EstadoRuta
import kotlin.test.Test
import kotlin.test.assertEquals

/** [construirRecomendacionRequest] es la logica que arma el pedido real a
 * /api/ia/recomendacion (ver ApioRepository.obtenerRecomendacionIa): aca se
 * prueba sin red, sin backend y sin la API key de Gemini. */
class ApioRepositoryTest {

    private val destino = Hospital("Hospital Honorio Delgado", "MINSA", LatLon(-16.4, -71.53))

    private val ruta = EstadoRuta(
        destino = destino,
        distanciaM = 7400.0,
        tiempoS = 720.0,
        tiempoSConTrafico = 1020.0,
        privilegiosCruzados = listOf(
            PrivilegioCruzado(
                avenida = "Av. Ejercito",
                general = 7.0,
                especificos = Especificos(carrilExclusivo = 8.2),
                tipoPrincipal = "carril_exclusivo",
                tipoPrincipalScore = 8.2,
            ),
        ),
        avenidasTraficoCruzadas = listOf("Av. Ejercito", "Av. Independencia"),
        puntos = emptyList(),
    )

    private val riesgo = EvaluacionRiesgo(nivel = NivelRiesgo.MEDIO, mensaje = "Riesgo medio", puntaje = 0.48)

    @Test
    fun establecimiento_del_backend_se_convierte_en_hospital() {
        val hospital = aHospital(
            EstablecimientoDto(
                codigo = "00012345",
                nombre = "Hospital Goyeneche",
                tipo = "MINSA",
                categoria = "II-2",
                emergencia = true,
                distrito = "Arequipa",
                lat = -16.40,
                lon = -71.52,
            ),
        )

        assertEquals("00012345", hospital.codigo)
        assertEquals("II-2", hospital.categoria)
        assertEquals(true, hospital.emergencia)
        assertEquals(-16.40, hospital.posicion.lat)
        assertEquals(-71.52, hospital.posicion.lon)
    }

    @Test
    fun traslado_guarda_destino_tiempos_y_el_privilegio_de_mayor_puntaje() {
        val traslado = construirTraslado(ruta, fechaMs = 1_000L)

        assertEquals("Hospital Honorio Delgado", traslado.destino)
        assertEquals("MINSA", traslado.tipoDestino)
        assertEquals(1_000L, traslado.fechaMs)
        assertEquals(1020.0, traslado.tiempoSConTrafico)
        assertEquals("carril_exclusivo", traslado.privilegioPrincipal)
    }

    @Test
    fun usa_el_nivel_de_trafico_real_para_cada_avenida_de_la_ruta() {
        val trafico = listOf(
            AvenidaTrafico(avenida = "Av. Ejercito", ratio = 0.4, nivel = "alta"),
            AvenidaTrafico(avenida = "Av. Independencia", ratio = 0.9, nivel = "baja"),
        )

        val datos = construirRecomendacionRequest(ruta, trafico, riesgo)

        assertEquals("alta", datos.avenidasTrafico.first { it.avenida == "Av. Ejercito" }.nivel)
        assertEquals("baja", datos.avenidasTrafico.first { it.avenida == "Av. Independencia" }.nivel)
    }

    @Test
    fun usa_desconocido_cuando_el_trafico_no_tiene_esa_avenida() {
        val datos = construirRecomendacionRequest(ruta, trafico = emptyList(), riesgo = riesgo)

        assertEquals(listOf("desconocido", "desconocido"), datos.avenidasTrafico.map { it.nivel })
    }

    @Test
    fun arma_destino_eta_y_riesgo_desde_la_ruta_y_la_heuristica() {
        val datos = construirRecomendacionRequest(ruta, trafico = emptyList(), riesgo = riesgo)

        assertEquals("Hospital Honorio Delgado", datos.destino)
        assertEquals(720.0, datos.tiempoS)
        assertEquals(1020.0, datos.tiempoSConTrafico)
        assertEquals("MEDIO", datos.riesgoNivel)
        assertEquals(0.48, datos.riesgoPuntaje)
        assertEquals("carril_exclusivo", datos.privilegios.single().tipo)
        assertEquals(8.2, datos.privilegios.single().score)
    }
}
