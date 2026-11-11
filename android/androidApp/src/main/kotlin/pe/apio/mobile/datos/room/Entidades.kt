package pe.apio.mobile.datos.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Establecimiento de salud guardado para usar la app sin red. La clave es
 * el codigo IPRESS (o el nombre, para los que solo vienen de OSM). */
@Entity(tableName = "hospitales")
data class HospitalEntity(
    @PrimaryKey val clave: String,
    val nombre: String,
    val tipo: String,
    val lat: Double,
    val lon: Double,
    val codigo: String?,
    val categoria: String?,
    val emergencia: Boolean,
    val distrito: String?,
    val telefono: String?,
)

/** Ultima ruta calculada hacia cada hospital (una fila por destino). */
@Entity(tableName = "ultima_ruta")
data class UltimaRutaEntity(
    @PrimaryKey @ColumnInfo(name = "destino_nombre") val destinoNombre: String,
    @ColumnInfo(name = "destino_tipo") val destinoTipo: String,
    @ColumnInfo(name = "destino_lat") val destinoLat: Double,
    @ColumnInfo(name = "destino_lon") val destinoLon: Double,
    @ColumnInfo(name = "distancia_m") val distanciaM: Double,
    @ColumnInfo(name = "tiempo_s") val tiempoS: Double,
    @ColumnInfo(name = "tiempo_s_con_trafico") val tiempoSConTrafico: Double,
    // listas guardadas como texto con los TypeConverters de Convertidores.kt
    @ColumnInfo(name = "avenidas_trafico") val avenidasTrafico: List<String>,
    val puntos: List<PuntoRuta>,
)

/** Punto de la polilinea de la ruta (version local de LatLon para Room). */
data class PuntoRuta(val lat: Double, val lon: Double)

/** Un traslado del historial (pantallas Historial y Perfil). */
@Entity(tableName = "traslados")
data class TrasladoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val destino: String,
    @ColumnInfo(name = "tipo_destino") val tipoDestino: String,
    @ColumnInfo(name = "fecha_ms") val fechaMs: Long,
    @ColumnInfo(name = "distancia_m") val distanciaM: Double,
    @ColumnInfo(name = "tiempo_s") val tiempoS: Double,
    @ColumnInfo(name = "tiempo_s_con_trafico") val tiempoSConTrafico: Double,
    @ColumnInfo(name = "privilegio_principal") val privilegioPrincipal: String?,
)
