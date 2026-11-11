package pe.apio.mobile.datos.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Base de datos local de APIO con Room. Guarda los establecimientos de
 * salud, la ultima ruta por destino y el historial de traslados, para que
 * la app siga mostrando datos cuando no hay red.
 */
@Database(
    entities = [HospitalEntity::class, UltimaRutaEntity::class, TrasladoEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Convertidores::class)
abstract class ApioDatabase : RoomDatabase() {

    abstract fun hospitalDao(): HospitalDao
    abstract fun rutaDao(): RutaDao
    abstract fun trasladoDao(): TrasladoDao

    companion object {
        // archivo distinto al apio.db del SQLiteOpenHelper anterior
        private const val NOMBRE_BD = "apio_room.db"

        @Volatile
        private var instancia: ApioDatabase? = null

        /** Una sola instancia para toda la app (patron singleton). */
        fun obtener(context: Context): ApioDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    ApioDatabase::class.java,
                    NOMBRE_BD,
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instancia = it }
            }
    }
}
