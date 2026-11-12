package pe.apio.mobile.plataforma

import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import java.util.Locale

/**
 * Texto a voz de Android. El motor tarda un momento en iniciar: lo que se
 * pida decir antes de que este listo se guarda y se dice apenas arranca.
 */
private class LocutorAndroid : Locutor {
    var motor: TextToSpeech? = null
    private var listo = false
    private var pendiente: String? = null
    private var numero = 0

    fun alIniciar(estado: Int) {
        val tts = motor ?: return
        if (estado != TextToSpeech.SUCCESS) return
        // espanol de Peru; si el celular no lo tiene, el espanol que haya
        val resultado = tts.setLanguage(Locale("es", "PE"))
        if (resultado == TextToSpeech.LANG_MISSING_DATA || resultado == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts.setLanguage(Locale("es"))
        }
        listo = true
        pendiente?.let(::decir)
        pendiente = null
    }

    override fun decir(texto: String) {
        val tts = motor
        if (tts == null || !listo) {
            pendiente = texto
            return
        }
        // QUEUE_ADD: un aviso nuevo espera a que termine el anterior
        tts.speak(texto, TextToSpeech.QUEUE_ADD, null, "apio-${numero++}")
    }

    fun cerrar() {
        listo = false
        motor?.stop()
        motor?.shutdown()
        motor = null
    }
}

@Composable
actual fun rememberLocutor(): Locutor {
    val contexto = LocalContext.current.applicationContext
    val locutor = remember {
        LocutorAndroid().also { nuevo ->
            nuevo.motor = TextToSpeech(contexto) { estado -> nuevo.alIniciar(estado) }
        }
    }
    DisposableEffect(locutor) {
        onDispose { locutor.cerrar() }
    }
    return locutor
}

@Composable
actual fun MantenerPantallaEncendida(activo: Boolean) {
    val vista = LocalView.current
    DisposableEffect(vista, activo) {
        vista.keepScreenOn = activo
        onDispose { vista.keepScreenOn = false }
    }
}
