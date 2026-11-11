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
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import pe.apio.mobile.modelo.LatLon
import pe.apio.mobile.modelo.PosicionGps

private const val CANAL_ID = "apio_seguimiento"
private const val NOTIFICACION_ID = 1001
// Una lectura por segundo: el mapa desliza la ambulancia de una posicion a la
// siguiente, y con lecturas mas espaciadas se veria avanzar a tirones.
private const val INTERVALO_MIN_MS = 1_000L
private const val DISTANCIA_MIN_M = 2f

/**
 * Servicio en primer plano que pide la ubicacion GPS real del dispositivo
 * mientras esta activo el "modo ambulancia real" (boton en App.kt). Sin un
 * foreground service, Android corta las actualizaciones de ubicacion en
 * cuanto la app pasa a segundo plano -- por eso el permiso de ubicacion y
 * la notificacion persistente son necesarios para el criterio de
 * "Servicios" de la rubrica (ver CLAUDE.md).
 */
class SeguimientoUbicacionServicio : Service() {

    private val _posicion = MutableStateFlow<PosicionGps?>(null)

    /** Ultima lectura aceptada: punto + rumbo y velocidad si el GPS los da. */
    val posicion: StateFlow<PosicionGps?> = _posicion

    private lateinit var administradorUbicacion: LocationManager

    private val listener = LocationListener { location: Location -> aceptarUbicacion(location) }

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
        // GPS solo da posicion al aire libre y puede tardar minutos en el
        // primer fix; la red (Wi-Fi/antenas) responde en segundos aunque sea
        // menos precisa. Se piden las dos y se queda con la mas precisa.
        val proveedores = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { administradorUbicacion.isProviderEnabled(it) }
        if (proveedores.isEmpty()) {
            // ubicacion del celular apagada en Ajustes
            stopSelf()
            return
        }
        // arranca con la ultima posicion conocida para no esperar al primer fix
        proveedores
            .mapNotNull { administradorUbicacion.getLastKnownLocation(it) }
            .maxByOrNull { it.time }
            ?.let(::aceptarUbicacion)
        proveedores.forEach { proveedor ->
            administradorUbicacion.requestLocationUpdates(
                proveedor,
                INTERVALO_MIN_MS,
                DISTANCIA_MIN_M,
                listener,
                Looper.getMainLooper(),
            )
        }
    }

    private var ultimaAceptada: Location? = null

    /** Descarta una lectura de red imprecisa si hace poco llego una de GPS
     * mejor, para que la ambulancia no salte de un lado a otro. */
    private fun aceptarUbicacion(location: Location) {
        val anterior = ultimaAceptada
        val reciente = anterior != null && location.time - anterior.time < 10_000L
        if (reciente && location.accuracy > anterior!!.accuracy * 2) return
        ultimaAceptada = location
        _posicion.value = PosicionGps(
            punto = LatLon(location.latitude, location.longitude),
            // el rumbo del GPS es el mas fiable para orientar la ambulancia y
            // la camara; la ubicacion por red no lo trae
            rumbo = if (location.hasBearing()) location.bearing else null,
            velocidadMs = if (location.hasSpeed()) location.speed else null,
        )
    }

    override fun onDestroy() {
        administradorUbicacion.removeUpdates(listener)
        super.onDestroy()
    }
}
