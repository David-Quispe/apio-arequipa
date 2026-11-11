package pe.apio.mobile.ui.theme

import androidx.compose.ui.graphics.Color

// Paleta sacada de las variables del archivo de Figma "APIO - Diseno de
// vistas (Material 3)": azul institucional como primario (confianza,
// servicio de salud) y naranja como terciario para lo que exige atencion
// (contraflujo, trafico moderado).

// ---- Esquema claro ----
val AzulPrimario = Color(0xFF004790)
val AzulPrimarioContenedor = Color(0xFFD6E3FF)
val AzulSobrePrimarioContenedor = Color(0xFF001B3E)
val Secundario = Color(0xFF4C5F81)
val SecundarioContenedor = Color(0xFFC2D5FD)
val SobreSecundarioContenedor = Color(0xFF2F4365)
val NaranjaTerciario = Color(0xFF994700)
val NaranjaTerciarioContenedor = Color(0xFFFFDBC8)
val SobreTerciarioContenedor = Color(0xFF321300)
val ErrorClaro = Color(0xFFBA1A1A)
val ErrorContenedorClaro = Color(0xFFFFDAD6)
val SuperficieClara = Color(0xFFF9F9FF)
val SobreSuperficieClara = Color(0xFF191C21)
val SobreSuperficieVarianteClara = Color(0xFF424752)
val ContenedorMasBajoClaro = Color(0xFFFFFFFF)
val ContenedorBajoClaro = Color(0xFFF2F3FB)
val ContenedorClaro = Color(0xFFEDEDF5)
val ContenedorAltoClaro = Color(0xFFE7E8EF)
val ContenedorMasAltoClaro = Color(0xFFE1E2E9)
val ContornoClaro = Color(0xFF727783)
val ContornoVarianteClaro = Color(0xFFC2C6D4)

// ---- Esquema oscuro (tonos equivalentes de la misma paleta) ----
val AzulPrimarioOscuro = Color(0xFFA9C7FF)
val SobrePrimarioOscuro = Color(0xFF003061)
val AzulPrimarioContenedorOscuro = Color(0xFF00468C)
val SecundarioOscuro = Color(0xFFB4C8EE)
val SecundarioContenedorOscuro = Color(0xFF344767)
val NaranjaTerciarioOscuro = Color(0xFFFFB68A)
val TerciarioContenedorOscuro = Color(0xFF763400)
val ErrorOscuro = Color(0xFFFFB4AB)
val ErrorContenedorOscuro = Color(0xFF93000A)
val SuperficieOscura = Color(0xFF111318)
val SobreSuperficieOscura = Color(0xFFE1E2E9)
val SobreSuperficieVarianteOscura = Color(0xFFC2C6D4)
val ContenedorMasBajoOscuro = Color(0xFF0C0E13)
val ContenedorBajoOscuro = Color(0xFF191C20)
val ContenedorOscuro = Color(0xFF1D2024)
val ContenedorAltoOscuro = Color(0xFF282A2F)
val ContenedorMasAltoOscuro = Color(0xFF33353A)
val ContornoOscuro = Color(0xFF8C909E)

// ---- Colores semanticos fijos ----
// Significan lo mismo en tema claro y oscuro (fluido / moderado / pesado),
// por eso no salen del colorScheme: se usan con transparencia sobre la
// superficie para que se lean bien en los dos temas.
val ApioVerde = Color(0xFF1E8E5A)
val ApioAmbar = Color(0xFFD97706)
val ApioRojoAlerta = Color(0xFFDC2626)
val ApioGrisSinDatos = Color(0xFF9CA3AF)
