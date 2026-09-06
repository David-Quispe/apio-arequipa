package pe.apio.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch
import pe.apio.mobile.red.ApioApiClient

// Base URL de desarrollo: 10.0.2.2 es el alias del emulador Android hacia
// el localhost de la maquina host, donde corre el backend real (ver
// AndroidManifest.xml / network_security_config.xml para el permiso de
// trafico sin cifrar hacia esta IP).
private const val API_BASE_URL = "http://10.0.2.2:8000/api"

// TEMPORAL (paso 2 del plan: spike de red, sin mapa todavia) -- se
// reemplaza en el paso 6 por la pantalla real con mapa, ruteo y paneles.
@Composable
@Preview
fun App() {
    val apiClient = remember { ApioApiClient(API_BASE_URL) }
    val scope = rememberCoroutineScope()
    var resultado by remember { mutableStateOf("Todavia no se consulto el backend.") }

    MaterialTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(onClick = {
                resultado = "Consultando /api/traffic..."
                scope.launch {
                    resultado = try {
                        val trafico = apiClient.obtenerTrafico()
                        trafico.avenidas.joinToString("\n") { "${it.avenida}: ${it.nivel} (ratio ${it.ratio})" }
                    } catch (e: Exception) {
                        "Error: ${e.message}"
                    }
                }
            }) {
                Text("Probar /api/traffic")
            }
            Text(resultado)
        }
    }
}