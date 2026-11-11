package pe.apio.mobile.datos.room

import androidx.room.TypeConverter

/** Room solo guarda tipos simples: las listas se pasan a texto y de vuelta. */
class Convertidores {

    @TypeConverter
    fun textosALista(valor: String): List<String> =
        if (valor.isEmpty()) emptyList() else valor.split(SEPARADOR_TEXTOS)

    @TypeConverter
    fun listaATextos(lista: List<String>): String = lista.joinToString(SEPARADOR_TEXTOS)

    @TypeConverter
    fun textoAPuntos(valor: String): List<PuntoRuta> =
        if (valor.isEmpty()) {
            emptyList()
        } else {
            valor.split(";").map { par ->
                val (lat, lon) = par.split(",")
                PuntoRuta(lat.toDouble(), lon.toDouble())
            }
        }

    @TypeConverter
    fun puntosATexto(puntos: List<PuntoRuta>): String =
        puntos.joinToString(";") { "${it.lat},${it.lon}" }

    private companion object {
        const val SEPARADOR_TEXTOS = "|"
    }
}
