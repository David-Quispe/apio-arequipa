package pe.apio.mobile.modelo

/**
 * Una lectura de ubicacion del dispositivo, agnostica de plataforma (la
 * arma SeguimientoUbicacionServicio a partir de android.location.Location).
 *
 * [rumbo] (grados, 0 = norte) y [velocidadMs] (metros por segundo) vienen
 * solo cuando el proveedor los informa: el GPS en movimiento si, la
 * ubicacion por red (Wi-Fi/antenas) no. Con null, el rumbo se calcula entre
 * posiciones (ver mapa/Rumbo.kt).
 */
data class PosicionGps(
    val punto: LatLon,
    val rumbo: Float? = null,
    val velocidadMs: Float? = null,
)
