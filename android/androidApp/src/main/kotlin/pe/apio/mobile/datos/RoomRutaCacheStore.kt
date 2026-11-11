package pe.apio.mobile.datos

import pe.apio.mobile.datos.room.ApioDatabase
import pe.apio.mobile.datos.room.HospitalEntity
import pe.apio.mobile.datos.room.PuntoRuta
import pe.apio.mobile.datos.room.TrasladoEntity
import pe.apio.mobile.datos.room.UltimaRutaEntity
import pe.apio.mobile.modelo.Hospital
import pe.apio.mobile.modelo.LatLon
import pe.apio.mobile.modelo.Traslado
import pe.apio.mobile.ui.EstadoRuta

/**
 * Implementacion Android de [RutaCacheStore] (declarada en el modulo
 * shared) sobre Room. Traduce entre los modelos de la app y las entidades
 * de la base de datos. La ultima ruta se guarda sin los privilegios
 * cruzados a proposito: si se usa el cache es porque no hay red, y lo que
 * importa es el camino y el ETA, no los chips de privilegio.
 */
class RoomRutaCacheStore(db: ApioDatabase) : RutaCacheStore {

    private val hospitalDao = db.hospitalDao()
    private val rutaDao = db.rutaDao()
    private val trasladoDao = db.trasladoDao()

    override suspend fun guardarHospitales(hospitales: List<Hospital>) {
        hospitalDao.reemplazarTodos(hospitales.map { it.aEntidad() })
    }

    override suspend fun obtenerHospitalesCache(): List<Hospital> =
        hospitalDao.obtenerTodos().map { it.aModelo() }

    override suspend fun guardarUltimaRuta(ruta: EstadoRuta) {
        rutaDao.guardar(
            UltimaRutaEntity(
                destinoNombre = ruta.destino.nombre,
                destinoTipo = ruta.destino.tipo,
                destinoLat = ruta.destino.posicion.lat,
                destinoLon = ruta.destino.posicion.lon,
                distanciaM = ruta.distanciaM,
                tiempoS = ruta.tiempoS,
                tiempoSConTrafico = ruta.tiempoSConTrafico,
                avenidasTrafico = ruta.avenidasTraficoCruzadas,
                puntos = ruta.puntos.map { PuntoRuta(it.lat, it.lon) },
            ),
        )
    }

    override suspend fun obtenerUltimaRutaCache(destinoNombre: String): EstadoRuta? {
        val guardada = rutaDao.obtenerPorDestino(destinoNombre) ?: return null
        return EstadoRuta(
            destino = Hospital(
                nombre = guardada.destinoNombre,
                tipo = guardada.destinoTipo,
                posicion = LatLon(guardada.destinoLat, guardada.destinoLon),
            ),
            distanciaM = guardada.distanciaM,
            tiempoS = guardada.tiempoS,
            tiempoSConTrafico = guardada.tiempoSConTrafico,
            privilegiosCruzados = emptyList(),
            avenidasTraficoCruzadas = guardada.avenidasTrafico,
            puntos = guardada.puntos.map { LatLon(it.lat, it.lon) },
        )
    }

    override suspend fun registrarTraslado(traslado: Traslado) {
        trasladoDao.insertar(
            TrasladoEntity(
                destino = traslado.destino,
                tipoDestino = traslado.tipoDestino,
                fechaMs = traslado.fechaMs,
                distanciaM = traslado.distanciaM,
                tiempoS = traslado.tiempoS,
                tiempoSConTrafico = traslado.tiempoSConTrafico,
                privilegioPrincipal = traslado.privilegioPrincipal,
            ),
        )
    }

    override suspend fun obtenerTraslados(limite: Int): List<Traslado> =
        trasladoDao.obtenerRecientes(limite).map {
            Traslado(
                destino = it.destino,
                tipoDestino = it.tipoDestino,
                fechaMs = it.fechaMs,
                distanciaM = it.distanciaM,
                tiempoS = it.tiempoS,
                tiempoSConTrafico = it.tiempoSConTrafico,
                privilegioPrincipal = it.privilegioPrincipal,
            )
        }
}

private fun Hospital.aEntidad() = HospitalEntity(
    clave = codigo ?: nombre,
    nombre = nombre,
    tipo = tipo,
    lat = posicion.lat,
    lon = posicion.lon,
    codigo = codigo,
    categoria = categoria,
    emergencia = emergencia,
    distrito = distrito,
    telefono = telefono,
)

private fun HospitalEntity.aModelo() = Hospital(
    nombre = nombre,
    tipo = tipo,
    posicion = LatLon(lat, lon),
    codigo = codigo,
    categoria = categoria,
    emergencia = emergencia,
    distrito = distrito,
    telefono = telefono,
)
