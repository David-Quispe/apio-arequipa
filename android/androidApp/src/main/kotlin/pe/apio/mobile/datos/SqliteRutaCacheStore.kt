package pe.apio.mobile.datos

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pe.apio.mobile.modelo.Hospital
import pe.apio.mobile.modelo.LatLon
import pe.apio.mobile.modelo.Traslado
import pe.apio.mobile.ui.EstadoRuta

/**
 * Implementacion Android de [RutaCacheStore] (declarada en el modulo
 * shared) sobre [ApioSqliteHelper]. La ultima ruta se guarda sin los
 * privilegios cruzados a proposito -- son datos secundarios de la UI (los
 * chips de privilegio); si el cache se usa es porque ya no hay red, y lo
 * que importa para seguir viendo la ambulancia moverse es el camino y el
 * ETA, no esos chips.
 */
class SqliteRutaCacheStore(private val helper: ApioSqliteHelper) : RutaCacheStore {

    override suspend fun guardarHospitales(hospitales: List<Hospital>) = withContext(Dispatchers.IO) {
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            // la lista del backend reemplaza a la anterior completa (asi no
            // quedan establecimientos dados de baja en RENIPRESS)
            db.delete(ApioSqliteHelper.TABLA_HOSPITALES, null, null)
            hospitales.forEach { hospital ->
                val valores = ContentValues().apply {
                    put("clave", hospital.codigo ?: hospital.nombre)
                    put("nombre", hospital.nombre)
                    put("tipo", hospital.tipo)
                    put("lat", hospital.posicion.lat)
                    put("lon", hospital.posicion.lon)
                    put("codigo", hospital.codigo)
                    put("categoria", hospital.categoria)
                    put("emergencia", if (hospital.emergencia) 1 else 0)
                    put("distrito", hospital.distrito)
                    put("telefono", hospital.telefono)
                }
                db.insertWithOnConflict(
                    ApioSqliteHelper.TABLA_HOSPITALES,
                    null,
                    valores,
                    SQLiteDatabase.CONFLICT_REPLACE,
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    override suspend fun obtenerHospitalesCache(): List<Hospital> = withContext(Dispatchers.IO) {
        helper.readableDatabase
            .query(ApioSqliteHelper.TABLA_HOSPITALES, null, null, null, null, null, null)
            .use { cursor ->
                val resultado = mutableListOf<Hospital>()
                fun texto(columna: String): String? {
                    val idx = cursor.getColumnIndexOrThrow(columna)
                    return if (cursor.isNull(idx)) null else cursor.getString(idx)
                }
                while (cursor.moveToNext()) {
                    resultado += Hospital(
                        nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre")),
                        tipo = cursor.getString(cursor.getColumnIndexOrThrow("tipo")),
                        posicion = LatLon(
                            cursor.getDouble(cursor.getColumnIndexOrThrow("lat")),
                            cursor.getDouble(cursor.getColumnIndexOrThrow("lon")),
                        ),
                        codigo = texto("codigo"),
                        categoria = texto("categoria"),
                        emergencia = cursor.getInt(cursor.getColumnIndexOrThrow("emergencia")) == 1,
                        distrito = texto("distrito"),
                        telefono = texto("telefono"),
                    )
                }
                resultado
            }
    }

    override suspend fun guardarUltimaRuta(ruta: EstadoRuta) = withContext(Dispatchers.IO) {
        val valores = ContentValues().apply {
            put("destino_nombre", ruta.destino.nombre)
            put("destino_tipo", ruta.destino.tipo)
            put("destino_lat", ruta.destino.posicion.lat)
            put("destino_lon", ruta.destino.posicion.lon)
            put("distancia_m", ruta.distanciaM)
            put("tiempo_s", ruta.tiempoS)
            put("tiempo_s_con_trafico", ruta.tiempoSConTrafico)
            put("avenidas_trafico", ruta.avenidasTraficoCruzadas.joinToString("|"))
            put("puntos", ruta.puntos.joinToString(";") { "${it.lat},${it.lon}" })
        }
        helper.writableDatabase.insertWithOnConflict(
            ApioSqliteHelper.TABLA_ULTIMA_RUTA,
            null,
            valores,
            SQLiteDatabase.CONFLICT_REPLACE,
        )
        Unit
    }

    override suspend fun obtenerUltimaRutaCache(destinoNombre: String): EstadoRuta? = withContext(Dispatchers.IO) {
        helper.readableDatabase.query(
            ApioSqliteHelper.TABLA_ULTIMA_RUTA,
            null,
            "destino_nombre = ?",
            arrayOf(destinoNombre),
            null,
            null,
            null,
        ).use { cursor ->
            if (!cursor.moveToFirst()) return@withContext null
            val avenidasTexto = cursor.getString(cursor.getColumnIndexOrThrow("avenidas_trafico"))
            val puntosTexto = cursor.getString(cursor.getColumnIndexOrThrow("puntos"))
            EstadoRuta(
                destino = Hospital(
                    nombre = cursor.getString(cursor.getColumnIndexOrThrow("destino_nombre")),
                    tipo = cursor.getString(cursor.getColumnIndexOrThrow("destino_tipo")),
                    posicion = LatLon(
                        cursor.getDouble(cursor.getColumnIndexOrThrow("destino_lat")),
                        cursor.getDouble(cursor.getColumnIndexOrThrow("destino_lon")),
                    ),
                ),
                distanciaM = cursor.getDouble(cursor.getColumnIndexOrThrow("distancia_m")),
                tiempoS = cursor.getDouble(cursor.getColumnIndexOrThrow("tiempo_s")),
                tiempoSConTrafico = cursor.getDouble(cursor.getColumnIndexOrThrow("tiempo_s_con_trafico")),
                privilegiosCruzados = emptyList(),
                avenidasTraficoCruzadas = if (avenidasTexto.isEmpty()) emptyList() else avenidasTexto.split("|"),
                puntos = if (puntosTexto.isEmpty()) {
                    emptyList()
                } else {
                    puntosTexto.split(";").map { par ->
                        val (lat, lon) = par.split(",")
                        LatLon(lat.toDouble(), lon.toDouble())
                    }
                },
            )
        }
    }

    // ---- Historial de traslados ----

    override suspend fun registrarTraslado(traslado: Traslado) = withContext(Dispatchers.IO) {
        val valores = ContentValues().apply {
            put("destino", traslado.destino)
            put("tipo_destino", traslado.tipoDestino)
            put("fecha_ms", traslado.fechaMs)
            put("distancia_m", traslado.distanciaM)
            put("tiempo_s", traslado.tiempoS)
            put("tiempo_s_con_trafico", traslado.tiempoSConTrafico)
            if (traslado.privilegioPrincipal != null) {
                put("privilegio_principal", traslado.privilegioPrincipal)
            } else {
                putNull("privilegio_principal")
            }
        }
        helper.writableDatabase.insert(ApioSqliteHelper.TABLA_TRASLADOS, null, valores)
        Unit
    }

    override suspend fun obtenerTraslados(limite: Int): List<Traslado> = withContext(Dispatchers.IO) {
        helper.readableDatabase.query(
            ApioSqliteHelper.TABLA_TRASLADOS,
            null,
            null,
            null,
            null,
            null,
            "fecha_ms DESC",
            limite.toString(),
        ).use { cursor ->
            val resultado = mutableListOf<Traslado>()
            val idxPrivilegio = cursor.getColumnIndexOrThrow("privilegio_principal")
            while (cursor.moveToNext()) {
                resultado += Traslado(
                    destino = cursor.getString(cursor.getColumnIndexOrThrow("destino")),
                    tipoDestino = cursor.getString(cursor.getColumnIndexOrThrow("tipo_destino")),
                    fechaMs = cursor.getLong(cursor.getColumnIndexOrThrow("fecha_ms")),
                    distanciaM = cursor.getDouble(cursor.getColumnIndexOrThrow("distancia_m")),
                    tiempoS = cursor.getDouble(cursor.getColumnIndexOrThrow("tiempo_s")),
                    tiempoSConTrafico = cursor.getDouble(cursor.getColumnIndexOrThrow("tiempo_s_con_trafico")),
                    privilegioPrincipal = if (cursor.isNull(idxPrivilegio)) null else cursor.getString(idxPrivilegio),
                )
            }
            resultado
        }
    }
}
