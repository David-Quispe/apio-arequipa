package pe.apio.mobile.modelo

/** Punto agnostico de plataforma -- el LatLng de MapLibre es Android-only, no
 * sirve en commonMain (necesario para poder sumar iOS despues). */
data class LatLon(val lat: Double, val lon: Double)

/** Establecimiento de salud. Los campos extra vienen de RENIPRESS (via
 * /api/establecimientos); tienen valor por defecto para que la lista local
 * HOSPITALES y el cache viejo sigan funcionando. */
data class Hospital(
    val nombre: String,
    val tipo: String, // "MINSA" | "EsSalud" | "Privada" | "FFAA/PNP" | "Municipal" | "Otro"
    val posicion: LatLon,
    val codigo: String? = null, // codigo IPRESS de SUSALUD
    val categoria: String? = null, // "I-1" ... "III-2"
    val emergencia: Boolean = true, // destino valido para una ambulancia
    val distrito: String? = null,
    val telefono: String? = null,
)
