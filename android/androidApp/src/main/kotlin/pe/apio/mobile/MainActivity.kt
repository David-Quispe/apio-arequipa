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
import pe.apio.mobile.datos.ApioSqliteHelper
import pe.apio.mobile.datos.SqliteRutaCacheStore
import pe.apio.mobile.red.ApioApiClient
import pe.apio.mobile.servicio.SeguimientoUbicacionServicio

// 10.0.2.2 es el alias del emulador Android hacia el localhost de la
// maquina host, donde corre el backend real.
private const val API_BASE_URL = "http://10.0.2.2:8000/api"

/**
 * Ademas de alojar el Composable [App], esta Activity es quien sabe de
 * Android "de verdad": arma el cache SQLite (ver datos/ApioSqliteHelper),
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

        setContent {
            App(
                crearRepositorio = {
                    val cache = SqliteRutaCacheStore(ApioSqliteHelper(applicationContext))
                    ApioRepository(ApioApiClient(API_BASE_URL), cache)
                },
                ubicacionReal = servicioUbicacion?.ubicacion,
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
