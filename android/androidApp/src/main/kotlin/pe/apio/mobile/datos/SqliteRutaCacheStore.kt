package pe.apio.mobile.datos

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pe.apio.mobile.modelo.Hospital
import pe.apio.mobile.modelo.LatLon
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
            hospitales.forEach { hospital ->
                val valores = ContentValues().apply {
                    put("nombre", hospital.nombre)
                    put("tipo", hospital.tipo)
                    put("lat", hospital.posicion.lat)
                    put("lon", hospital.posicion.lon)
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
                while (cursor.moveToNext()) {
                    resultado += Hospital(
                        nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre")),
                        tipo = cursor.getString(cursor.getColumnIndexOrThrow("tipo")),
                        posicion = LatLon(
                            cursor.getDouble(cursor.getColumnIndexOrThrow("lat")),
                            cursor.getDouble(cursor.getColumnIndexOrThrow("lon")),
                        ),
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
}
