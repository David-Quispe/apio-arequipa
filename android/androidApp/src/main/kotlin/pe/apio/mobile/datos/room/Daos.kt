package pe.apio.mobile.datos.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface HospitalDao {

    @Query("SELECT * FROM hospitales ORDER BY emergencia DESC, nombre")
    suspend fun obtenerTodos(): List<HospitalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodos(hospitales: List<HospitalEntity>)

    @Query("DELETE FROM hospitales")
    suspend fun borrarTodos()

    /** La lista del backend reemplaza a la anterior completa (asi no quedan
     * establecimientos dados de baja en RENIPRESS). */
    @Transaction
    suspend fun reemplazarTodos(hospitales: List<HospitalEntity>) {
        borrarTodos()
        insertarTodos(hospitales)
    }
}

@Dao
interface RutaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(ruta: UltimaRutaEntity)

    @Query("SELECT * FROM ultima_ruta WHERE destino_nombre = :destinoNombre LIMIT 1")
    suspend fun obtenerPorDestino(destinoNombre: String): UltimaRutaEntity?
}

@Dao
interface TrasladoDao {

    @Insert
    suspend fun insertar(traslado: TrasladoEntity)

    @Query("SELECT * FROM traslados ORDER BY fecha_ms DESC LIMIT :limite")
    suspend fun obtenerRecientes(limite: Int): List<TrasladoEntity>
}
