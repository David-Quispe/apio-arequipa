package pe.apio.mobile.datos

import pe.apio.mobile.modelo.Hospital
import pe.apio.mobile.ui.EstadoRuta

/**
 * Puerto de cache que implementa cada plataforma (en Android, con SQLite
 * nativo -- ver androidApp/.../datos/SqliteRutaCacheStore.kt). El modulo
 * shared no sabe nada de Android ni de bases de datos: solo pide/guarda
 * estos datos para que [ApioRepository] pueda responder sin red.
 */
interface RutaCacheStore {
    suspend fun guardarHospitales(hospitales: List<Hospital>)
    suspend fun obtenerHospitalesCache(): List<Hospital>

    suspend fun guardarUltimaRuta(ruta: EstadoRuta)
    suspend fun obtenerUltimaRutaCache(destinoNombre: String): EstadoRuta?
}
