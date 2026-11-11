package pe.apio.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val EsquemaClaro = lightColorScheme(
    primary = AzulPrimario,
    onPrimary = Color.White,
    primaryContainer = AzulPrimarioContenedor,
    onPrimaryContainer = AzulSobrePrimarioContenedor,
    secondary = Secundario,
    onSecondary = Color.White,
    secondaryContainer = SecundarioContenedor,
    onSecondaryContainer = SobreSecundarioContenedor,
    tertiary = NaranjaTerciario,
    onTertiary = Color.White,
    tertiaryContainer = NaranjaTerciarioContenedor,
    onTertiaryContainer = SobreTerciarioContenedor,
    error = ErrorClaro,
    onError = Color.White,
    errorContainer = ErrorContenedorClaro,
    onErrorContainer = Color(0xFF410002),
    background = SuperficieClara,
    onBackground = SobreSuperficieClara,
    surface = SuperficieClara,
    onSurface = SobreSuperficieClara,
    surfaceVariant = ContenedorAltoClaro,
    onSurfaceVariant = SobreSuperficieVarianteClara,
    outline = ContornoClaro,
    outlineVariant = ContornoVarianteClaro,
    surfaceContainerLowest = ContenedorMasBajoClaro,
    surfaceContainerLow = ContenedorBajoClaro,
    surfaceContainer = ContenedorClaro,
    surfaceContainerHigh = ContenedorAltoClaro,
    surfaceContainerHighest = ContenedorMasAltoClaro,
)

private val EsquemaOscuro = darkColorScheme(
    primary = AzulPrimarioOscuro,
    onPrimary = SobrePrimarioOscuro,
    primaryContainer = AzulPrimarioContenedorOscuro,
    onPrimaryContainer = AzulPrimarioContenedor,
    secondary = SecundarioOscuro,
    onSecondary = Color(0xFF1D314F),
    secondaryContainer = SecundarioContenedorOscuro,
    onSecondaryContainer = AzulPrimarioContenedor,
    tertiary = NaranjaTerciarioOscuro,
    onTertiary = Color(0xFF532200),
    tertiaryContainer = TerciarioContenedorOscuro,
    onTertiaryContainer = NaranjaTerciarioContenedor,
    error = ErrorOscuro,
    onError = Color(0xFF690005),
    errorContainer = ErrorContenedorOscuro,
    onErrorContainer = ErrorContenedorClaro,
    background = SuperficieOscura,
    onBackground = SobreSuperficieOscura,
    surface = SuperficieOscura,
    onSurface = SobreSuperficieOscura,
    surfaceVariant = ContenedorAltoOscuro,
    onSurfaceVariant = SobreSuperficieVarianteOscura,
    outline = ContornoOscuro,
    outlineVariant = SobreSuperficieVarianteClara,
    surfaceContainerLowest = ContenedorMasBajoOscuro,
    surfaceContainerLow = ContenedorBajoOscuro,
    surfaceContainer = ContenedorOscuro,
    surfaceContainerHigh = ContenedorAltoOscuro,
    surfaceContainerHighest = ContenedorMasAltoOscuro,
)

// Jerarquia del diseno de Figma: numeros grandes y titulos en negrita (en el
// diseno van en Montserrat; aca se usa la fuente del sistema con el mismo
// peso para no cargar archivos de fuente extra).
private val base = Typography()
private val TipografiaApio = Typography(
    displayMedium = base.displayMedium.copy(fontWeight = FontWeight.Bold, fontSize = 52.sp, lineHeight = 56.sp),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Medium),
)

/**
 * Tema Material 3 de APIO con la paleta del diseno de Figma. [oscuro] por
 * defecto sigue al sistema, pero la pantalla de Perfil puede forzarlo con su
 * switch de "Tema oscuro".
 */
@Composable
fun ApioTheme(
    oscuro: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (oscuro) EsquemaOscuro else EsquemaClaro,
        typography = TipografiaApio,
        content = content,
    )
}
