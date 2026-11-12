package pe.apio.mobile.plataforma

import androidx.compose.runtime.Composable

/** Dice mensajes cortos en voz alta (texto a voz del dispositivo). */
interface Locutor {
    fun decir(texto: String)
}

/**
 * Locutor ligado a la pantalla que lo pide: se crea una vez y se libera al
 * salir. Si el dispositivo no tiene voz en espanol, simplemente no habla.
 */
@Composable
expect fun rememberLocutor(): Locutor

/**
 * Mientras [activo] sea true la pantalla no se apaga sola. Se usa en modo
 * navegacion: quien maneja no puede estar tocando el celular para despertarlo.
 */
@Composable
expect fun MantenerPantallaEncendida(activo: Boolean)
