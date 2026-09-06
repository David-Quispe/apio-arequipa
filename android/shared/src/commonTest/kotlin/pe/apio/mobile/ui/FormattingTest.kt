package pe.apio.mobile.ui

import pe.apio.mobile.modelo.Especificos
import pe.apio.mobile.modelo.PrivilegioCruzado
import kotlin.test.Test
import kotlin.test.assertEquals

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
