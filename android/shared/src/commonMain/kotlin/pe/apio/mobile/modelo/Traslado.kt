package pe.apio.mobile.modelo

/**
 * Un traslado registrado en el historial local: cada vez que se calcula una
 * ruta a un hospital se guarda uno (ver ApioRepository.registrarTraslado).
 * Se persiste en SQLite en Android (tabla "traslados" de ApioSqliteHelper)
 * y alimenta las pantallas de Historial y Perfil.
 */
data class Traslado(
    val destino: String,
    val tipoDestino: String,
    val fechaMs: Long,
    val distanciaM: Double,
    val tiempoS: Double,
    val tiempoSConTrafico: Double,
    val privilegioPrincipal: String?,
)
