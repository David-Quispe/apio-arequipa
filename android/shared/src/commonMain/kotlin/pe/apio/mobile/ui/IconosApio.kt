package pe.apio.mobile.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Iconos del set Material Symbols que usa el diseno de Figma, dibujados como
 * ImageVector a partir de su path SVG (viewport 24x24). Asi no hace falta
 * sumar la dependencia material-icons-extended, que es pesada y ya no se
 * publica para las versiones nuevas de Compose Multiplatform.
 */
private fun icono(nombre: String, vararg trazos: String): ImageVector {
    val builder = ImageVector.Builder(
        name = nombre,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    )
    trazos.forEach { trazo ->
        builder.addPath(pathData = addPathNodes(trazo), fill = SolidColor(Color.Black))
    }
    return builder.build()
}

object IconosApio {
    val Mapa: ImageVector by lazy {
        icono(
            "mapa",
            "M20.5,3l-0.16,0.03L15,5.1 9,3 3.36,4.9c-0.21,0.07 -0.36,0.25 -0.36,0.48V20.5c0,0.28 0.22,0.5 0.5,0.5l0.16,-0.03L9,18.9l6,2.1 5.64,-1.9c0.21,-0.07 0.36,-0.25 0.36,-0.48V3.5c0,-0.28 -0.22,-0.5 -0.5,-0.5zM15,19l-6,-2.11V5l6,2.11V19z",
        )
    }
    val Hospital: ImageVector by lazy {
        icono(
            "hospital",
            "M19,3H5c-1.1,0 -1.99,0.9 -1.99,2L3,19c0,1.1 0.9,2 2,2h14c1.1,0 2,-0.9 2,-2V5c0,-1.1 -0.9,-2 -2,-2zM18,14h-4v4h-4v-4H6v-4h4V6h4v4h4v4z",
        )
    }
    val Historial: ImageVector by lazy {
        icono(
            "historial",
            "M13,3c-4.97,0 -9,4.03 -9,9L1,12l3.89,3.89 0.07,0.14L9,12L6,12c0,-3.87 3.13,-7 7,-7s7,3.13 7,7 -3.13,7 -7,7c-1.93,0 -3.68,-0.79 -4.94,-2.06l-1.42,1.42C8.27,19.99 10.51,21 13,21c4.97,0 9,-4.03 9,-9s-4.03,-9 -9,-9zM12,8v5l4.28,2.54 0.72,-1.21 -3.5,-2.08L13.5,8L12,8z",
        )
    }
    val Persona: ImageVector by lazy {
        icono(
            "persona",
            "M12,12c2.21,0 4,-1.79 4,-4s-1.79,-4 -4,-4 -4,1.79 -4,4 1.79,4 4,4zM12,14c-2.67,0 -8,1.34 -8,4v2h16v-2c0,-2.66 -5.33,-4 -8,-4z",
        )
    }
    val MiUbicacion: ImageVector by lazy {
        icono(
            "mi_ubicacion",
            "M12,8c-2.21,0 -4,1.79 -4,4s1.79,4 4,4 4,-1.79 4,-4 -1.79,-4 -4,-4zM20.94,11c-0.46,-4.17 -3.77,-7.48 -7.94,-7.94L13,1h-2v2.06C6.83,3.52 3.52,6.83 3.06,11L1,11v2h2.06c0.46,4.17 3.77,7.48 7.94,7.94L11,23h2v-2.06c4.17,-0.46 7.48,-3.77 7.94,-7.94L23,13v-2h-2.06zM12,19c-3.87,0 -7,-3.13 -7,-7s3.13,-7 7,-7 7,3.13 7,7 -3.13,7 -7,7z",
        )
    }
    val Buscar: ImageVector by lazy {
        icono(
            "buscar",
            "M15.5,14h-0.79l-0.28,-0.27C15.41,12.59 16,11.11 16,9.5 16,5.91 13.09,3 9.5,3S3,5.91 3,9.5 5.91,16 9.5,16c1.61,0 3.09,-0.59 4.23,-1.57l0.27,0.28v0.79l5,4.99L20.49,19l-4.99,-5zM9.5,14C7.01,14 5,11.99 5,9.5S7.01,5 9.5,5 14,7.01 14,9.5 11.99,14 9.5,14z",
        )
    }
    val Cerrar: ImageVector by lazy {
        icono(
            "cerrar",
            "M19,6.41L17.59,5 12,10.59 6.41,5 5,6.41 10.59,12 5,17.59 6.41,19 12,13.41 17.59,19 19,17.59 13.41,12z",
        )
    }
    val Alerta: ImageVector by lazy {
        icono("alerta", "M1,21h22L12,2 1,21zM13,18h-2v-2h2v2zM13,14h-2v-4h2v4z")
    }
    val Navegacion: ImageVector by lazy {
        icono("navegacion", "M12,2L4.5,20.29l0.71,0.71L12,18l6.79,3 0.71,-0.71z")
    }
    val Ruta: ImageVector by lazy {
        icono(
            "ruta",
            "M19,15.18V7c0,-2.21 -1.79,-4 -4,-4s-4,1.79 -4,4v10c0,1.1 -0.9,2 -2,2s-2,-0.9 -2,-2V8.82C8.16,8.4 9,7.3 9,6c0,-1.66 -1.34,-3 -3,-3S3,4.34 3,6c0,1.3 0.84,2.4 2,2.82V17c0,2.21 1.79,4 4,4s4,-1.79 4,-4V7c0,-1.1 0.9,-2 2,-2s2,0.9 2,2v8.18c-1.16,0.41 -2,1.51 -2,2.82c0,1.66 1.34,3 3,3s3,-1.34 3,-3C21,16.7 20.16,15.6 19,15.18z",
        )
    }
    val Trafico: ImageVector by lazy {
        icono(
            "trafico",
            "M20,10h-3V8.86c1.72,-0.45 3,-2 3,-3.86h-3V4c0,-0.55 -0.45,-1 -1,-1H8c-0.55,0 -1,0.45 -1,1v1H4c0,1.86 1.28,3.41 3,3.86V10H4c0,1.86 1.28,3.41 3,3.86V15H4c0,1.86 1.28,3.41 3,3.86V20c0,0.55 0.45,1 1,1h8c0.55,0 1,-0.45 1,-1v-1.14c1.72,-0.45 3,-2 3,-3.86h-3v-1.14c1.72,-0.45 3,-2 3,-3.86zM12,19c-1.11,0 -2,-0.9 -2,-2s0.89,-2 2,-2c1.1,0 2,0.9 2,2s-0.89,2 -2,2zM12,14c-1.11,0 -2,-0.9 -2,-2s0.89,-2 2,-2c1.1,0 2,0.9 2,2s-0.89,2 -2,2zM12,9c-1.11,0 -2,-0.9 -2,-2 0,-1.11 0.89,-2 2,-2 1.1,0 2,0.89 2,2 0,1.1 -0.89,2 -2,2z",
        )
    }
    val Ia: ImageVector by lazy {
        icono(
            "ia",
            "M19,9l1.25,-2.75L23,5l-2.75,-1.25L19,1l-1.25,2.75L15,5l2.75,1.25L19,9zM11.5,9.5L9,4 6.5,9.5 1,12l5.5,2.5L9,20l2.5,-5.5L17,12l-5.5,-2.5zM19,15l-1.25,2.75L15,19l2.75,1.25L19,23l1.25,-2.75L23,19l-2.75,-1.25L19,15z",
        )
    }
    val TendenciaBaja: ImageVector by lazy {
        icono("tendencia_baja", "M16,18l2.29,-2.29 -4.88,-4.88 -4,4L2,7.41 3.41,6l6,6 4,-4 6.3,6.29L22,12v6z")
    }
    val Check: ImageVector by lazy {
        icono(
            "check",
            "M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM10,17l-5,-5 1.41,-1.41L10,14.17l7.59,-7.59L19,8l-9,9z",
        )
    }
    val ModoOscuro: ImageVector by lazy {
        icono(
            "modo_oscuro",
            "M12,3c-4.97,0 -9,4.03 -9,9s4.03,9 9,9 9,-4.03 9,-9c0,-0.46 -0.04,-0.92 -0.1,-1.36 -0.98,1.37 -2.58,2.26 -4.4,2.26 -2.98,0 -5.4,-2.42 -5.4,-5.4 0,-1.81 0.89,-3.42 2.26,-4.4C12.92,3.04 12.46,3 12,3z",
        )
    }
    val Emergencia: ImageVector by lazy {
        icono(
            "emergencia",
            "M20.79,9.23l-2,-3.46L14,8.54V3h-4v5.54L5.21,5.77l-2,3.46L8,12l-4.79,2.77 2,3.46L10,15.46V21h4v-5.46l4.79,2.77 2,-3.46L16,12z",
        )
    }
}
