package pe.apio.mobile.mapa

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

// Lado del sprite en dp. Es cuadrado y la ambulancia va centrada, asi al
// rotarlo con icon-rotate gira sobre su propio centro.
private const val LADO_DP = 72

/**
 * Sprite propio de la ambulancia (dibujado a mano con Canvas, sin iconos de
 * terceros): vista desde arriba, con el frente apuntando al norte (0 grados).
 *
 * Es el "falso 3D" del modo navegacion: la capa del mapa lo dibuja acostado
 * sobre la calle (icon-pitch-alignment = map) y lo rota segun el rumbo, asi
 * que con la camara inclinada se ve en perspectiva como un vehiculo real,
 * pero sigue siendo una imagen plana, liviana para celulares de gama baja.
 */
fun crearBitmapAmbulancia(context: Context): Bitmap {
    val densidad = context.resources.displayMetrics.density
    val lado = (LADO_DP * densidad).toInt()
    val bitmap = Bitmap.createBitmap(lado, lado, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    // u = una unidad de la grilla de 72 x 72 sobre la que esta pensado el dibujo
    val u = lado / LADO_DP.toFloat()

    fun rect(izq: Float, arriba: Float, der: Float, abajo: Float) =
        RectF(izq * u, arriba * u, der * u, abajo * u)

    val relleno = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    val borde = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * u
        color = Color.parseColor("#64748B")
    }

    // Espejos retrovisores (van primero: quedan debajo de la carroceria)
    relleno.color = Color.parseColor("#334155")
    canvas.drawRoundRect(rect(18.5f, 17f, 23f, 21f), 1.5f * u, 1.5f * u, relleno)
    canvas.drawRoundRect(rect(49f, 17f, 53.5f, 21f), 1.5f * u, 1.5f * u, relleno)

    // Carroceria blanca con sombra, para despegarla del mapa
    val carroceria = rect(22f, 7f, 50f, 65f)
    relleno.color = Color.WHITE
    relleno.setShadowLayer(4f * u, 0f, 1.5f * u, Color.parseColor("#80000000"))
    canvas.drawRoundRect(carroceria, 7f * u, 7f * u, relleno)
    relleno.clearShadowLayer()
    canvas.drawRoundRect(carroceria, 7f * u, 7f * u, borde)

    // Parabrisas (adelante) y luneta trasera
    relleno.color = Color.parseColor("#1E293B")
    canvas.drawRoundRect(rect(25f, 13f, 47f, 22f), 3f * u, 3f * u, relleno)
    canvas.drawRoundRect(rect(27f, 59.5f, 45f, 62.5f), 1.5f * u, 1.5f * u, relleno)

    // Barra de luces azul sobre el techo de la cabina
    relleno.color = Color.parseColor("#1D4ED8")
    canvas.drawRoundRect(rect(26f, 24.5f, 46f, 29f), 2f * u, 2f * u, relleno)
    relleno.color = Color.parseColor("#93C5FD")
    canvas.drawRoundRect(rect(33f, 25.5f, 39f, 28f), 1f * u, 1f * u, relleno)

    // Franjas rojas a los costados del modulo sanitario
    relleno.color = Color.parseColor("#DC2626")
    canvas.drawRect(rect(23.5f, 32f, 26f, 57f), relleno)
    canvas.drawRect(rect(46f, 32f, 48.5f, 57f), relleno)

    // Cruz roja en el techo
    canvas.drawRoundRect(rect(33.5f, 36f, 38.5f, 53f), 1f * u, 1f * u, relleno)
    canvas.drawRoundRect(rect(27.5f, 42f, 44.5f, 47f), 1f * u, 1f * u, relleno)

    return bitmap
}
