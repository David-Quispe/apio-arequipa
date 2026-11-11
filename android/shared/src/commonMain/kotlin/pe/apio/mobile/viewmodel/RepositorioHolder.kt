package pe.apio.mobile.viewmodel

import androidx.lifecycle.ViewModel
import pe.apio.mobile.datos.ApioRepository

/**
 * Guarda UN solo [ApioRepository] para los tres ViewModels (Mapa,
 * Hospitales e Historial), asi comparten el mismo cliente Retrofit y la
 * misma base Room. Al ser un ViewModel sobrevive a las rotaciones, y cierra
 * la red una sola vez cuando la pantalla se destruye de verdad.
 */
class RepositorioHolder(val repositorio: ApioRepository) : ViewModel() {
    override fun onCleared() {
        repositorio.cerrar()
    }
}
