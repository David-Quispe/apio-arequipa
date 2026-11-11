package pe.apio.mobile.ui

import pe.apio.mobile.modelo.Especificos
import pe.apio.mobile.modelo.LatLon
import pe.apio.mobile.modelo.PrivilegioCruzado
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FormattingTest {

    @Test
    fun distancia_redondea_a_2_decimales() {
        assertEquals("4.46", formatearDecimal(4.462396, 2))
        assertEquals("4.46 km", formatearDistanciaKm(4462.396))
    }

    @Test
    fun minutos_redondea_a_1_decimal_y_no_pierde_el_cero() {
        assertEquals("6.9 min", formatearMinutos(414.0))
        assertEquals("6.0 min", formatearMinutos(360.0))
    }

    @Test
    fun hora_local_usa_utc_menos_5() {
        // 2026-11-10T15:30:00Z -> 10:30 en Lima
        assertEquals("10:30", horaLocal(1_794_324_600_000L))
    }

    @Test
    fun etiqueta_dia_hoy_ayer_y_antes() {
        assertEquals("Hoy", etiquetaDia(dia = 100, hoy = 100))
        assertEquals("Ayer", etiquetaDia(dia = 99, hoy = 100))
        assertEquals("Hace 3 días", etiquetaDia(dia = 97, hoy = 100))
    }

    @Test
    fun distancia_linea_recta_entre_dos_hospitales_es_razonable() {
        // Honorio Delgado -> Goyeneche: ~1.4 km en linea recta
        val d = distanciaLineaRectaM(LatLon(-16.4149129, -71.5315582), LatLon(-16.4024925, -71.5279176))
        assertTrue(d in 1_300.0..1_550.0, "distancia fuera de rango: $d")
    }

    @Test
    fun privilegios_formatea_como_en_la_pwa() {
        val privilegios = listOf(
            PrivilegioCruzado(
                avenida = "Av. Ejercito",
                general = 5.9,
                especificos = Especificos(carrilExclusivo = 6.6),
                tipoPrincipal = "Carril exclusivo",
                tipoPrincipalScore = 6.6,
            ),
            PrivilegioCruzado(
                avenida = "Puente Grau",
                general = 5.2,
                especificos = Especificos(contraflujo = 9.0),
                tipoPrincipal = "Contraflujo",
                tipoPrincipalScore = 9.0,
            ),
        )
        assertEquals(
            "Av. Ejercito (Carril exclusivo 6.6/10), Puente Grau (Contraflujo 9.0/10)",
            formatearPrivilegios(privilegios),
        )
    }
}
