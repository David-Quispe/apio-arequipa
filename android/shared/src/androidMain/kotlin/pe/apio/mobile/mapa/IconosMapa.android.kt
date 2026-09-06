package pe.apio.mobile.mapa

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable

/**
 * Icono circular (color de fondo + emoji encima, con borde blanco y sombra)
 * para los marcadores del mapa -- mismo estilo que crearIconoCircular en
 * frontend/src/App.jsx, hecho a mano con Canvas porque osmdroid necesita un
 * Drawable/Bitmap concreto, no puede reusar HTML/CSS como Leaflet.
 */
fun crearIconoCircular(context: Context, emoji: String, colorHex: String, sizeDp: Int = 34): Drawable {
    val densidad = context.resources.displayMetrics.density
    val sizePx = (sizeDp * densidad)
    val bitmap = Bitmap.createBitmap(sizePx.toInt(), sizePx.toInt(), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val centro = sizePx / 2f
    val radio = centro - (2f * densidad)

    val paintCirculo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor(colorHex)
        setShadowLayer(4f * densidad, 0f, densidad, Color.parseColor("#73000000"))
    }
    canvas.drawCircle(centro, centro, radio, paintCirculo)

    val paintBorde = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2f * densidad
    }
    canvas.drawCircle(centro, centro, radio, paintBorde)

    val paintTexto = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = sizePx * 0.5f
        textAlign = Paint.Align.CENTER
    }
    val yTexto = centro - (paintTexto.descent() + paintTexto.ascent()) / 2
    canvas.drawText(emoji, centro, yTexto, paintTexto)

    return BitmapDrawable(context.resources, bitmap)
}
