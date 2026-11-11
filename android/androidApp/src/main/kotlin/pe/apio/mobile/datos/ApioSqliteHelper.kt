package pe.apio.mobile.datos

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Capa de persistencia local del lado Android (sin Room): Room necesita un
 * procesador de anotaciones (KSP o kapt) y, a la fecha de este entregable,
 * KSP todavia no publica una build para el Kotlin que usa este proyecto
 * (2.4.10 -- la ultima version publicada en Maven Central es 2.3.13) y kapt
 * (org.jetbrains.kotlin.kapt) es incompatible con el Kotlin integrado de
 * AGP 9 que compila :androidApp (ver guia de Android Developers sobre
 * migracion a "built-in Kotlin"). Esta clase replica a mano la misma
 * arquitectura de Room -- Entity/DAO/Database -- sobre SQLiteOpenHelper (el
 * mismo motor SQLite que usa Room por debajo). Ver CLAUDE.md, criterio
 * "Datos", para la decision completa.
 */
class ApioSqliteHelper(context: Context) : SQLiteOpenHelper(context, NOMBRE_BD, null, VERSION_BD) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLA_HOSPITALES (
                clave TEXT PRIMARY KEY NOT NULL,
                nombre TEXT NOT NULL,
                tipo TEXT NOT NULL,
                lat REAL NOT NULL,
                lon REAL NOT NULL,
                codigo TEXT,
                categoria TEXT,
                emergencia INTEGER NOT NULL DEFAULT 1,
                distrito TEXT,
                telefono TEXT
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE $TABLA_ULTIMA_RUTA (
                destino_nombre TEXT PRIMARY KEY NOT NULL,
                destino_tipo TEXT NOT NULL,
                destino_lat REAL NOT NULL,
                destino_lon REAL NOT NULL,
                distancia_m REAL NOT NULL,
                tiempo_s REAL NOT NULL,
                tiempo_s_con_trafico REAL NOT NULL,
                avenidas_trafico TEXT NOT NULL,
                puntos TEXT NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE $TABLA_TRASLADOS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                destino TEXT NOT NULL,
                tipo_destino TEXT NOT NULL,
                fecha_ms INTEGER NOT NULL,
                distancia_m REAL NOT NULL,
                tiempo_s REAL NOT NULL,
                tiempo_s_con_trafico REAL NOT NULL,
                privilegio_principal TEXT
            )
            """.trimIndent(),
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLA_HOSPITALES")
        db.execSQL("DROP TABLE IF EXISTS $TABLA_ULTIMA_RUTA")
        db.execSQL("DROP TABLE IF EXISTS $TABLA_TRASLADOS")
        onCreate(db)
    }

    companion object {
        private const val NOMBRE_BD = "apio.db"
        // v2: tabla de traslados para el historial
        // v3: hospitales con datos de RENIPRESS (codigo, categoria, emergencia...)
        private const val VERSION_BD = 3
        const val TABLA_HOSPITALES = "hospitales"
        const val TABLA_ULTIMA_RUTA = "ultima_ruta"
        const val TABLA_TRASLADOS = "traslados"
    }
}
