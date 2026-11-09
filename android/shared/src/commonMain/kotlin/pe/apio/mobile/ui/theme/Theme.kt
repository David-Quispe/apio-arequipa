package pe.apio.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val EsquemaClaro = lightColorScheme(
    primary = ApioRojo,
    onPrimary = Color.White,
    secondary = ApioAzul,
    onSecondary = Color.White,
    tertiary = ApioVerde,
    onTertiary = Color.White,
    error = ApioRojoAlerta,
    onError = Color.White,
    background = Color(0xFFF9FAFB),
    onBackground = ApioGrisOscuro,
    surface = Color.White,
    onSurface = ApioGrisOscuro,
    surfaceVariant = ApioGrisClaro,
)

private val EsquemaOscuro = darkColorScheme(
    primary = ApioRojo,
    onPrimary = Color.White,
    secondary = ApioAzul,
    onSecondary = Color.White,
    tertiary = ApioVerde,
    onTertiary = Color.White,
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    background = Color(0xFF0F172A),
    onBackground = Color(0xFFE5E7EB),
    surface = Color(0xFF1E293B),
    onSurface = Color(0xFFE5E7EB),
    surfaceVariant = Color(0xFF334155),
)

/**
 * Tema Material 3 propio de APIO (reemplaza el MaterialTheme por defecto de
 * App.kt): misma paleta rojo/azul/verde que ya usan el mapa y los paneles,
 * con variante oscura automatica segun el tema del sistema.
 */
@Composable
fun ApioTheme(content: @Composable () -> Unit) {
    val esquema = if (isSystemInDarkTheme()) EsquemaOscuro else EsquemaClaro
    MaterialTheme(colorScheme = esquema, content = content)
}
