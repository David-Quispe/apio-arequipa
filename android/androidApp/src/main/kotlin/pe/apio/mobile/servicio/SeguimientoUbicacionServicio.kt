package pe.apio.mobile.servicio

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import pe.apio.mobile.modelo.LatLon

private const val CANAL_ID = "apio_seguimiento"
private const val NOTIFICACION_ID = 1001
private const val INTERVALO_MIN_MS = 3_000L
private const val DISTANCIA_MIN_M = 5f

/**
 * Servicio en primer plano que pide la ubicacion GPS real del dispositivo
 * mientras esta activo el "modo ambulancia real" (boton en App.kt). Sin un
 * foreground service, Android corta las actualizaciones de ubicacion en
 * cuanto la app pasa a segundo plano -- por eso el permiso de ubicacion y
 * la notificacion persistente son necesarios para el criterio de
 * "Servicios" de la rubrica (ver CLAUDE.md).
 */
class SeguimientoUbicacionServicio : Service() {

    private val _ubicacion = MutableStateFlow<LatLon?>(null)
    val ubicacion: StateFlow<LatLon?> = _ubicacion

    private lateinit var administradorUbicacion: LocationManager

    private val listener = LocationListener { location: Location ->
        _ubicacion.value = LatLon(location.latitude, location.longitude)
    }

    inner class EnlaceLocal : Binder() {
        fun servicio(): SeguimientoUbicacionServicio = this@SeguimientoUbicacionServicio
    }

    private val enlace = EnlaceLocal()

    override fun onBind(intent: Intent?): IBinder = enlace

    override fun onCreate() {
        super.onCreate()
        administradorUbicacion = getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        iniciarEnPrimerPlano()
        iniciarActualizacionesUbicacion()
        return START_STICKY
    }

    private fun iniciarEnPrimerPlano() {
        crearCanalNotificacion()
        val notificacion = NotificationCompat.Builder(this, CANAL_ID)
            .setContentTitle("APIO siguiendo tu ubicacion")
            .setContentText("La ambulancia se mueve en el mapa con tu GPS real")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICACION_ID, notificacion, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICACION_ID, notificacion)
        }
    }

    private fun crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val administrador = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            administrador.createNotificationChannel(
                NotificationChannel(CANAL_ID, "Seguimiento de ruta", NotificationManager.IMPORTANCE_LOW),
            )
        }
    }

    private fun iniciarActualizacionesUbicacion() {
        val concedido = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        if (!concedido) {
            stopSelf()
            return
        }
        administradorUbicacion.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            INTERVALO_MIN_MS,
            DISTANCIA_MIN_M,
            listener,
        )
    }

    override fun onDestroy() {
        administradorUbicacion.removeUpdates(listener)
        super.onDestroy()
    }
}
