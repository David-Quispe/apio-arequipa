package pe.apio.mobile.ui

import pe.apio.mobile.modelo.Hospital

/** Los 4 estados de la pantalla principal (ver el diseño de referencia del
 * proyecto): sin ruta elegida, calculando, ruta calculada, o fallo. Un solo
 * sealed type en vez de 3 variables sueltas (`cargando`/`error`/`ruta`)
 * porque son mutuamente excluyentes y el panel necesita saber, durante la
 * carga y el error, qué hospital se tocó (para mostrar su nombre y para el
 * botón de reintentar). */
sealed interface EstadoPantalla {
    data object Vacio : EstadoPantalla
    data class Cargando(val hospital: Hospital) : EstadoPantalla
    data class Exito(val ruta: EstadoRuta) : EstadoPantalla
    data class Error(val mensaje: String, val hospital: Hospital) : EstadoPantalla
}
