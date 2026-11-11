package pe.apio.mobile

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import pe.apio.mobile.datos.ApioRepository
import pe.apio.mobile.datos.RoomRutaCacheStore
import pe.apio.mobile.datos.room.ApioDatabase
import pe.apio.mobile.mapa.ConfigMapa
import pe.apio.mobile.red.RetrofitApioApi
import pe.apio.mobile.servicio.SeguimientoUbicacionServicio

// Backend desplegado en la VM de Azure (NGINX + HTTPS, dominio DuckDNS):
// funciona desde cualquier celular con datos o Wi-Fi.
private const val API_BASE_URL = "https://apio-arequipa.duckdns.org/api"

// Para desarrollo con el backend en tu PC y el emulador, usa esta otra
// (10.0.2.2 es el alias del emulador hacia el localhost del host):
// private const val API_BASE_URL = "http://10.0.2.2:8000/api"

/**
 * Ademas de alojar el Composable [App], esta Activity es quien sabe de
 * Android "de verdad": arma la red con Retrofit, el cache con Room (ver datos/room),
 * y liga/desliga el foreground service de ubicacion real pidiendo el
 * permiso en tiempo de ejecucion. [App] en shared no conoce nada de esto --
 * solo recibe una fabrica de [ApioRepository] y un Flow de ubicacion.
 */
class MainActivity : ComponentActivity() {

    private var ubicacionActiva by mutableStateOf(false)
    private var servicioUbicacion by mutableStateOf<SeguimientoUbicacionServicio?>(null)
    private var ligadoAlServicio = false

    private val conexionServicio = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val local = binder as SeguimientoUbicacionServicio.EnlaceLocal
            servicioUbicacion = local.servicio()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            servicioUbicacion = null
        }
    }

    private val pedirPermisos = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { resultados ->
        if (resultados[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            iniciarSeguimientoReal()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // La clave de MapTiler la conoce solo este modulo (BuildConfig, leida
        // de local.properties); el mapa vive en :shared y la recibe por aqui.
        ConfigMapa.claveMapTiler = BuildConfig.MAPTILER_KEY

        setContent {
            App(
                crearRepositorio = {
                    // Retrofit para la red y Room para el cache local
                    val cache = RoomRutaCacheStore(ApioDatabase.obtener(applicationContext))
                    ApioRepository(RetrofitApioApi(API_BASE_URL), cache)
                },
                ubicacionReal = servicioUbicacion?.posicion,
                ubicacionActiva = ubicacionActiva,
                onToggleUbicacion = ::alternarSeguimientoReal,
            )
        }
    }

    private fun alternarSeguimientoReal() {
        if (ubicacionActiva) {
            detenerSeguimientoReal()
            return
        }
        val concedido = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        if (concedido) {
            iniciarSeguimientoReal()
        } else {
            pedirPermisos.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.POST_NOTIFICATIONS),
            )
        }
    }

    private fun iniciarSeguimientoReal() {
        val intent = Intent(this, SeguimientoUbicacionServicio::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        bindService(intent, conexionServicio, Context.BIND_AUTO_CREATE)
        ligadoAlServicio = true
        ubicacionActiva = true
    }

    private fun detenerSeguimientoReal() {
        if (ligadoAlServicio) {
            unbindService(conexionServicio)
            ligadoAlServicio = false
        }
        servicioUbicacion = null
        stopService(Intent(this, SeguimientoUbicacionServicio::class.java))
        ubicacionActiva = false
    }

    override fun onDestroy() {
        if (ligadoAlServicio) {
            unbindService(conexionServicio)
            ligadoAlServicio = false
        }
        super.onDestroy()
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
