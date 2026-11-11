package pe.apio.mobile.mapa

/**
 * Configuracion del mapa que :shared no puede saber por si solo. La clave de
 * MapTiler sale de local.properties -> BuildConfig del modulo androidApp, y
 * MainActivity la deja aqui al arrancar (antes de dibujar el mapa).
 */
object ConfigMapa {
    var claveMapTiler: String = ""

    /**
     * URL del estilo vectorial segun el tema. Con clave se usa MapTiler
     * (streets-v2 y su variante oscura); sin clave se cae a OpenFreeMap, que
     * no la necesita, para que el mapa nunca quede en blanco.
     */
    fun urlEstilo(oscuro: Boolean): String {
        val clave = claveMapTiler.trim()
        if (clave.isEmpty()) {
            return if (oscuro) {
                "https://tiles.openfreemap.org/styles/dark"
            } else {
                "https://tiles.openfreemap.org/styles/liberty"
            }
        }
        val estilo = if (oscuro) "streets-v2-dark" else "streets-v2"
        return "https://api.maptiler.com/maps/$estilo/style.json?key=$clave"
    }
}
