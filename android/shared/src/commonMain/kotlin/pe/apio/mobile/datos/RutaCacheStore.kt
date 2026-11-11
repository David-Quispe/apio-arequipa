package pe.apio.mobile.datos

import pe.apio.mobile.modelo.Hospital
import pe.apio.mobile.modelo.Traslado
import pe.apio.mobile.ui.EstadoRuta

/**
 * Puerto de cache que implementa cada plataforma (en Android, con Room
 * -- ver androidApp/.../datos/RoomRutaCacheStore.kt). El modulo
 * shared no sabe nada de Android ni de bases de datos: solo pide/guarda
 * estos datos para que [ApioRepository] pueda responder sin red.
 */
interface RutaCacheStore {
    suspend fun guardarHospitales(hospitales: List<Hospital>)
    suspend fun obtenerHospitalesCache(): List<Hospital>

    suspend fun guardarUltimaRuta(ruta: EstadoRuta)
    suspend fun obtenerUltimaRutaCache(destinoNombre: String): EstadoRuta?

    // Historial de traslados (pantallas Historial y Perfil). Tienen una
    // implementacion por defecto vacia para que un cache que no soporte
    // historial (o un fake de tests) no este obligado a implementarlas.
    suspend fun registrarTraslado(traslado: Traslado) {}
    suspend fun obtenerTraslados(limite: Int): List<Traslado> = emptyList()
}
