package pe.apio.mobile.modelo

/** Punto agnostico de plataforma -- osmdroid's GeoPoint es Android-only, no
 * sirve en commonMain (necesario para poder sumar iOS despues). */
data class LatLon(val lat: Double, val lon: Double)

data class Hospital(
    val nombre: String,
    val tipo: String, // "MINSA" | "EsSalud" | "Privada"
    val posicion: LatLon,
)
