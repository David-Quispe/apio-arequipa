package pe.apio.mobile.mapa

import android.animation.ValueAnimator
import android.graphics.RectF
import android.view.Gravity
import android.view.animation.LinearInterpolator
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.FillExtrusionLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon
import pe.apio.mobile.modelo.COLOR_POR_TIPO
import pe.apio.mobile.modelo.Hospital
import pe.apio.mobile.modelo.LatLon
import pe.apio.mobile.ui.distanciaLineaRectaM

private const val FUENTE_RUTA = "ruta"
private const val FUENTE_HOSPITALES = "hospitales"
private const val FUENTE_AMBULANCIA = "ambulancia"
private const val FUENTE_RECORRIDO = "ruta-recorrida"
private const val CAPA_RECORRIDO = "ruta-recorrida-linea"
private const val CAPA_RUTA_BORDE = "ruta-borde"
private const val CAPA_RUTA_LINEA = "ruta-linea"
private const val CAPA_HOSPITALES = "hospitales-capa"
private const val CAPA_AMBULANCIA = "ambulancia-capa"
private const val FUENTE_AMBULANCIA_3D = "ambulancia-3d"
private const val CAPA_AMBULANCIA_3D = "ambulancia-3d-capa"

private const val IMAGEN_AMBULANCIA = "ambulancia"
private const val IMAGEN_HOSPITAL_OTRO = "hosp_Otro"
private const val IMAGEN_SIN_EMERGENCIA = "hosp_sin_emergencia"

// Propiedades de cada punto del GeoJSON, leidas por las capas con get(...)
private const val PROP_ICONO = "icono"
private const val PROP_INDICE = "indice"
private const val PROP_ORDEN = "orden"
private const val PROP_RUMBO = "rumbo"
private const val PROP_COLOR = "color"
private const val PROP_BASE = "base"
private const val PROP_ALTO = "alto"

// Largo en pantalla (dp) de la ambulancia 3D del modo navegacion. El modelo
// se escala segun el zoom para medir siempre esto, como el icono de un GPS.
private const val LARGO_AMBULANCIA_3D_DP = 58

private const val COLOR_TIPO_DESCONOCIDO = "#6b7280"
// Establecimientos sin emergencia (puestos y centros de salud I-1 a I-4):
// gris y mas chicos, para que no compitan con los destinos de ambulancia.
private const val COLOR_SIN_EMERGENCIA = "#9ca3af"
private const val TAMANO_HOSPITAL_DP = 34
private const val TAMANO_SIN_EMERGENCIA_DP = 22

// Ruta: azul primario del tema (Figma) con borde mas oscuro; de noche se
// aclara para que resalte sobre el mapa oscuro.
private const val COLOR_RUTA_DIA = "#004790"
private const val COLOR_RUTA_BORDE_DIA = "#001E45"
private const val COLOR_RUTA_NOCHE = "#8AB8FF"
private const val COLOR_RUTA_BORDE_NOCHE = "#0B2B5C"
// Tramo ya recorrido: gris, para que no compita con lo que falta
private const val COLOR_RECORRIDO_DIA = "#94A3B8"
private const val COLOR_RECORRIDO_NOCHE = "#64748B"

private const val ZOOM_INICIAL = 13.0
private const val ZOOM_NAVEGACION = 17.0
private const val INCLINACION_NAVEGACION = 55.0

// Lo que tarda la ambulancia (y la camara que la sigue) en deslizarse de una
// posicion GPS a la siguiente.
private const val DURACION_MOVIMIENTO_MS = 900
private const val DURACION_CAMBIO_CAMARA_MS = 1000

// Un cambio de posicion mas grande que esto no es "avanzar": es un salto
// (primer fix del GPS, tocar el mapa lejos) y se aplica sin animar.
private const val SALTO_SIN_ANIMAR_M = 500.0

// Navegando, si el GPS cae a menos de esto de la ruta se dibuja la ambulancia
// sobre la calle (el GPS tiembla unos metros). Mas lejos se respeta el GPS:
// probablemente se salio de la ruta.
private const val DISTANCIA_PEGAR_A_RUTA_M = 35.0
// Separacion minima entre puntos guardados del camino recorrido
private const val PASO_RECORRIDO_M = 3.0
private const val MAX_PUNTOS_RECORRIDO = 3000
// Cada cuanto alterna la barra de luces de la ambulancia 3D
private const val INTERVALO_DESTELLO_MS = 350L

// La camara del mapa sobrevive a que el composable salga de pantalla (cambiar
// de pestana recrea el MapView): al volver se retoma donde estaba.
private var camaraGuardada: CameraPosition? = null

/**
 * Todo lo imperativo de MapLibre en un solo lugar: estilo, imagenes, fuentes
 * GeoJSON, capas, animacion de la ambulancia y camara. El composable
 * [MapaApio] solo crea el MapView, respeta su ciclo de vida y llama a
 * [actualizar] con el estado mas reciente; aqui se compara con lo ya dibujado
 * y se toca el mapa solo en lo que cambio (nunca se recrea el MapView).
 *
 * Se usan fuentes y capas del estilo (no la API vieja de Marker): es lo que
 * permite rotar la ambulancia, acostarla sobre el mapa inclinado y dibujar
 * cientos de establecimientos sin un objeto por cada uno.
 */
internal class ControladorMapa(
    private val mapView: MapView,
    origenInicial: LatLon,
    rumboInicial: Float,
    private var oscuro: Boolean,
) {
    private val contexto = mapView.context
    private val densidad = contexto.resources.displayMetrics.density

    var alTocarMapa: (LatLon) -> Unit = {}
    var alTocarHospital: (Hospital) -> Unit = {}
    var alMoverCamaraConElDedo: () -> Unit = {}

    private var mapa: MapLibreMap? = null
    private var destruido = false

    // Fuentes del estilo actual; null mientras el estilo carga (o se recarga
    // al cambiar entre dia y noche).
    private var fuenteRuta: GeoJsonSource? = null
    private var fuenteHospitales: GeoJsonSource? = null
    private var fuenteAmbulancia: GeoJsonSource? = null
    private var fuenteAmbulancia3d: GeoJsonSource? = null
    // zoom con el que se dibujo el modelo 3D por ultima vez
    private var zoomModelo3d = -1.0

    // Ultimo estado pedido por la UI
    private var hospitales: List<Hospital> = emptyList()
    private var ruta: List<LatLon> = emptyList()
    private var modoNavegacion = false
    private var siguiendoCamara = true
    private var margenInferiorPx = 0

    // Ambulancia: donde esta dibujada ahora (se anima) y hacia donde va
    private var posicionDibujada = origenInicial
    private var rumboDibujado = rumboInicial
    private var posicionObjetivo = origenInicial
    private var rumboObjetivo = rumboInicial
    private var animador: ValueAnimator? = null

    // Progreso sobre la ruta mientras se navega
    private var fuenteRecorrido: GeoJsonSource? = null
    private var proyeccion: ProyeccionRuta? = null
    private val recorrido = mutableListOf<LatLon>()

    // Parpadeo de la barra de luces (solo corre en modo navegacion)
    private var destello = false
    private val tareaDestello = object : Runnable {
        override fun run() {
            if (destruido || !modoNavegacion) return
            destello = !destello
            pintarAmbulancia()
            mapView.postDelayed(this, INTERVALO_DESTELLO_MS)
        }
    }

    init {
        mapView.getMapAsync { mapaListo ->
            if (destruido) return@getMapAsync
            mapa = mapaListo
            configurarMapa(mapaListo)
            cargarEstilo()
            // actualizar() pudo haber pedido el modo navegacion antes de que
            // el mapa existiera: se aplica ahora, sin animacion.
            if (modoNavegacion && siguiendoCamara) camaraDeNavegacion(0, suave = false)
        }
    }

    // ---------------------------------------------------------- Entrada

    /** Lleva el mapa al estado que pide la UI. Se llama en cada recomposicion. */
    fun actualizar(
        origen: LatLon,
        rumbo: Float,
        hospitales: List<Hospital>,
        ruta: List<LatLon>,
        modoNavegacion: Boolean,
        siguiendoCamara: Boolean,
        oscuro: Boolean,
        margenInferiorPx: Int,
    ) {
        if (destruido) return

        if (oscuro != this.oscuro) {
            this.oscuro = oscuro
            cargarEstilo()
        }

        if (hospitales != this.hospitales) {
            this.hospitales = hospitales
            pintarHospitales()
        }

        // Un destino nuevo se reconoce por el ultimo punto de la ruta; los
        // recalculos por avance del GPS cambian el inicio, no el final.
        val destinoNuevo = ruta.isNotEmpty() && ruta.last() != this.ruta.lastOrNull()
        val rutaCambio = ruta != this.ruta
        this.ruta = ruta
        if (destinoNuevo) recorrido.clear()

        val entroANavegar = modoNavegacion && !this.modoNavegacion
        val salioDeNavegar = !modoNavegacion && this.modoNavegacion
        val retomoSeguimiento = siguiendoCamara && !this.siguiendoCamara
        this.modoNavegacion = modoNavegacion
        this.siguiendoCamara = siguiendoCamara
        // al entrar o salir de navegar se cambia entre el sprite plano y el modelo 3D
        if (entroANavegar || salioDeNavegar) pintarAmbulancia()
        this.margenInferiorPx = margenInferiorPx

        if (entroANavegar) {
            mapView.removeCallbacks(tareaDestello)
            mapView.postDelayed(tareaDestello, INTERVALO_DESTELLO_MS)
        }
        if (salioDeNavegar) mapView.removeCallbacks(tareaDestello)

        // Navegando, la ambulancia va pegada a la ruta y lo recorrido se pinta gris
        val posicion = seguirRuta(origen)
        if (rutaCambio || entroANavegar || salioDeNavegar || (modoNavegacion && posicion != posicionObjetivo)) {
            pintarRuta()
        }

        moverAmbulancia(posicion, rumbo)

        when {
            modoNavegacion && siguiendoCamara && (entroANavegar || retomoSeguimiento) ->
                camaraDeNavegacion(DURACION_CAMBIO_CAMARA_MS, suave = true)
            salioDeNavegar -> if (ruta.size >= 2) encuadrarRuta() else camaraPlana()
            !modoNavegacion && destinoNuevo -> encuadrarRuta()
        }
    }

    /** Suelta animaciones y recuerda la camara. El MapView lo destruye [MapaApio]. */
    fun destruir() {
        destruido = true
        mapView.removeCallbacks(tareaDestello)
        animador?.cancel()
        animador = null
        mapa?.let { camaraGuardada = it.cameraPosition }
        mapa = null
        fuenteRuta = null
        fuenteHospitales = null
        fuenteAmbulancia = null
        fuenteAmbulancia3d = null
    }

    // ---------------------------------------------------------- Mapa y estilo

    private fun configurarMapa(mapa: MapLibreMap) {
        mapa.uiSettings.apply {
            isCompassEnabled = false
            isLogoEnabled = false
            // La atribucion (MapTiler / OpenStreetMap) es obligatoria; va al
            // costado para que no la tapen el buscador ni la hoja de ruta.
            isAttributionEnabled = true
            attributionGravity = Gravity.START or Gravity.CENTER_VERTICAL
            val margen = (8 * densidad).toInt()
            setAttributionMargins(margen, 0, 0, 0)
        }

        val guardada = camaraGuardada
        mapa.cameraPosition = if (guardada != null) {
            // se retoma plana: si corresponde, actualizar() vuelve a inclinarla
            CameraPosition.Builder(guardada).tilt(0.0).bearing(0.0).padding(0.0, 0.0, 0.0, 0.0).build()
        } else {
            CameraPosition.Builder().target(posicionDibujada.aLatLng()).zoom(ZOOM_INICIAL).build()
        }

        mapa.addOnMapClickListener { punto ->
            val pantalla = mapa.projection.toScreenLocation(punto)
            // area de toque generosa: se maneja con el dedo, no con mouse
            val radio = 16 * densidad
            val zona = RectF(pantalla.x - radio, pantalla.y - radio, pantalla.x + radio, pantalla.y + radio)
            val hospital = mapa.queryRenderedFeatures(zona, CAPA_HOSPITALES)
                .filter { it.hasNonNullValueForProperty(PROP_INDICE) }
                // si hay varios encimados gana el que tiene emergencia
                .maxByOrNull { if (it.hasNonNullValueForProperty(PROP_ORDEN)) it.getNumberProperty(PROP_ORDEN).toInt() else 0 }
                ?.let { hospitales.getOrNull(it.getNumberProperty(PROP_INDICE).toInt()) }
            if (hospital != null) {
                alTocarHospital(hospital)
            } else {
                alTocarMapa(LatLon(punto.latitude, punto.longitude))
            }
            true
        }

        // Si el usuario arrastra el mapa, la UI deja de seguir a la ambulancia
        // y muestra el boton "Recentrar" (igual que Google Maps).
        // El modelo 3D se mide en metros: si cambia el zoom hay que rehacerlo
        // para que en pantalla siga del mismo tamano.
        mapa.addOnCameraMoveListener {
            if (modoNavegacion && kotlin.math.abs(mapa.cameraPosition.zoom - zoomModelo3d) > 0.02) {
                pintarAmbulancia()
            }
        }

        mapa.addOnCameraMoveStartedListener { motivo ->
            if (motivo == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) {
                alMoverCamaraConElDedo()
            }
        }
    }

    /**
     * Carga el estilo de dia o de noche. setStyle borra todo lo agregado al
     * estilo anterior (imagenes, fuentes, capas), asi que se reconstruye
     * completo en el callback.
     */
    private fun cargarEstilo() {
        val mapa = mapa ?: return
        fuenteRuta = null
        fuenteHospitales = null
        fuenteAmbulancia = null
        fuenteAmbulancia3d = null
        val oscuroPedido = oscuro
        mapa.setStyle(Style.Builder().fromUri(ConfigMapa.urlEstilo(oscuroPedido))) { estilo ->
            // si mientras cargaba se cambio otra vez de tema, este ya no sirve
            if (destruido || oscuroPedido != oscuro) return@setStyle
            agregarImagenes(estilo)
            agregarFuentesYCapas(estilo, oscuroPedido)
            pintarRuta()
            pintarHospitales()
            pintarAmbulancia()
        }
    }

    private fun agregarImagenes(estilo: Style) {
        estilo.addImage(IMAGEN_AMBULANCIA, crearBitmapAmbulancia(contexto))
        // Un icono por tipo de establecimiento, compartido por todos los puntos
        COLOR_POR_TIPO.forEach { (tipo, color) ->
            estilo.addImage("hosp_$tipo", crearBitmapCircular(contexto, "🏥", color, TAMANO_HOSPITAL_DP))
        }
        if (!COLOR_POR_TIPO.containsKey("Otro")) {
            estilo.addImage(
                IMAGEN_HOSPITAL_OTRO,
                crearBitmapCircular(contexto, "🏥", COLOR_TIPO_DESCONOCIDO, TAMANO_HOSPITAL_DP),
            )
        }
        estilo.addImage(
            IMAGEN_SIN_EMERGENCIA,
            crearBitmapCircular(contexto, "🏥", COLOR_SIN_EMERGENCIA, TAMANO_SIN_EMERGENCIA_DP),
        )
    }

    private fun agregarFuentesYCapas(estilo: Style, oscuro: Boolean) {
        val ruta = GeoJsonSource(FUENTE_RUTA)
        val hospitales = GeoJsonSource(FUENTE_HOSPITALES)
        val ambulancia = GeoJsonSource(FUENTE_AMBULANCIA)
        estilo.addSource(ruta)
        val recorrida = GeoJsonSource(FUENTE_RECORRIDO)
        estilo.addSource(recorrida)
        // debajo de la ruta: el tramo ya recorrido, mas fino y en gris
        estilo.addLayer(
            LineLayer(CAPA_RECORRIDO, FUENTE_RECORRIDO).withProperties(
                PropertyFactory.lineColor(if (oscuro) COLOR_RECORRIDO_NOCHE else COLOR_RECORRIDO_DIA),
                PropertyFactory.lineWidth(6f),
                PropertyFactory.lineOpacity(0.85f),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
            ),
        )
        fuenteRecorrido = recorrida
        estilo.addSource(hospitales)
        estilo.addSource(ambulancia)
        val ambulancia3d = GeoJsonSource(FUENTE_AMBULANCIA_3D)
        estilo.addSource(ambulancia3d)

        // El orden en que se agregan es el orden de dibujo: ruta abajo,
        // hospitales encima y la ambulancia siempre arriba de todo.
        estilo.addLayer(
            LineLayer(CAPA_RUTA_BORDE, FUENTE_RUTA).withProperties(
                PropertyFactory.lineColor(if (oscuro) COLOR_RUTA_BORDE_NOCHE else COLOR_RUTA_BORDE_DIA),
                PropertyFactory.lineWidth(10f),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
            ),
        )
        estilo.addLayer(
            LineLayer(CAPA_RUTA_LINEA, FUENTE_RUTA).withProperties(
                PropertyFactory.lineColor(if (oscuro) COLOR_RUTA_NOCHE else COLOR_RUTA_DIA),
                PropertyFactory.lineWidth(6f),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
            ),
        )
        estilo.addLayer(
            SymbolLayer(CAPA_HOSPITALES, FUENTE_HOSPITALES).withProperties(
                PropertyFactory.iconImage(Expression.get(PROP_ICONO)),
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconIgnorePlacement(true),
                // los que tienen emergencia se dibujan encima de los grises
                PropertyFactory.symbolSortKey(Expression.get(PROP_ORDEN)),
            ),
        )
        estilo.addLayer(
            SymbolLayer(CAPA_AMBULANCIA, FUENTE_AMBULANCIA).withProperties(
                PropertyFactory.iconImage(IMAGEN_AMBULANCIA),
                PropertyFactory.iconRotate(Expression.get(PROP_RUMBO)),
                // "map" en las dos: la imagen rota con el mapa (no con la
                // pantalla) y se acuesta sobre la calle cuando la camara se
                // inclina; ese es el efecto de falso 3D.
                PropertyFactory.iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_MAP),
                PropertyFactory.iconPitchAlignment(Property.ICON_PITCH_ALIGNMENT_MAP),
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconIgnorePlacement(true),
            ),
        )

        // Ambulancia 3D de verdad para el modo navegacion: bloques levantados
        // del suelo (fill-extrusion), cada uno con su color y altura. Con la
        // camara inclinada se le ven el frente, los costados y el techo.
        estilo.addLayer(
            FillExtrusionLayer(CAPA_AMBULANCIA_3D, FUENTE_AMBULANCIA_3D).withProperties(
                PropertyFactory.fillExtrusionColor(Expression.toColor(Expression.get(PROP_COLOR))),
                PropertyFactory.fillExtrusionBase(Expression.get(PROP_BASE)),
                PropertyFactory.fillExtrusionHeight(Expression.get(PROP_ALTO)),
                PropertyFactory.fillExtrusionOpacity(1f),
                // sin degradado vertical: caras de color parejo, se lee mejor
                PropertyFactory.fillExtrusionVerticalGradient(false),
            ),
        )

        fuenteRuta = ruta
        fuenteHospitales = hospitales
        fuenteAmbulancia = ambulancia
        fuenteAmbulancia3d = ambulancia3d
    }

    // ---------------------------------------------------------- Datos

    /**
     * Ubica la posicion del GPS respecto de la ruta mientras se navega: si
     * esta cerca la devuelve pegada a la calle, y va guardando el camino
     * hecho. Fuera de navegacion devuelve la posicion tal cual.
     */
    private fun seguirRuta(origen: LatLon): LatLon {
        if (!modoNavegacion) {
            proyeccion = null
            recorrido.clear()
            return origen
        }
        val cercana = proyectarEnRuta(ruta, origen)?.takeIf { it.distanciaM <= DISTANCIA_PEGAR_A_RUTA_M }
        proyeccion = cercana
        val posicion = cercana?.punto ?: origen
        val ultimo = recorrido.lastOrNull()
        if (ultimo == null || distanciaLineaRectaM(ultimo, posicion) >= PASO_RECORRIDO_M) {
            recorrido.add(posicion)
            if (recorrido.size > MAX_PUNTOS_RECORRIDO) recorrido.removeAt(0)
        }
        return posicion
    }

    private fun linea(puntos: List<LatLon>, fuente: GeoJsonSource) {
        if (puntos.size < 2) {
            fuente.setGeoJson(FeatureCollection.fromFeatures(emptyList<Feature>()))
        } else {
            fuente.setGeoJson(LineString.fromLngLats(puntos.map { Point.fromLngLat(it.lon, it.lat) }))
        }
    }

    /** En azul lo que falta por recorrer; en gris, lo ya recorrido. */
    private fun pintarRuta() {
        val fuente = fuenteRuta ?: return
        val porRecorrer = proyeccion?.let { rutaRestante(ruta, it) } ?: ruta
        linea(porRecorrer, fuente)
        fuenteRecorrido?.let { linea(if (modoNavegacion) recorrido.toList() else emptyList(), it) }
    }

    private fun pintarHospitales() {
        val fuente = fuenteHospitales ?: return
        val puntos = hospitales.mapIndexed { indice, hospital ->
            Feature.fromGeometry(Point.fromLngLat(hospital.posicion.lon, hospital.posicion.lat)).apply {
                addStringProperty(PROP_ICONO, hospital.imagen())
                // el indice permite saber que hospital se toco sin buscarlo por nombre
                addNumberProperty(PROP_INDICE, indice)
                addNumberProperty(PROP_ORDEN, if (hospital.emergencia) 1 else 0)
            }
        }
        fuente.setGeoJson(FeatureCollection.fromFeatures(puntos))
    }

    private fun Hospital.imagen(): String = when {
        !emergencia -> IMAGEN_SIN_EMERGENCIA
        COLOR_POR_TIPO.containsKey(tipo) -> "hosp_$tipo"
        else -> IMAGEN_HOSPITAL_OTRO
    }

    /**
     * Planificando (mapa plano visto desde arriba) la ambulancia es el sprite;
     * navegando (camara inclinada) es el modelo 3D. Solo una de las dos fuentes
     * tiene datos a la vez.
     */
    private fun pintarAmbulancia() {
        val fuente = fuenteAmbulancia ?: return
        val fuente3d = fuenteAmbulancia3d ?: return
        val vacio = FeatureCollection.fromFeatures(emptyList<Feature>())
        val mapa = mapa
        if (!modoNavegacion || mapa == null) {
            val punto = Feature.fromGeometry(Point.fromLngLat(posicionDibujada.lon, posicionDibujada.lat))
            punto.addNumberProperty(PROP_RUMBO, rumboDibujado)
            fuente.setGeoJson(punto)
            fuente3d.setGeoJson(vacio)
            zoomModelo3d = -1.0
            return
        }

        val zoom = mapa.cameraPosition.zoom
        zoomModelo3d = zoom
        val largoM = LARGO_AMBULANCIA_3D_DP * densidad * metrosPorPixel(posicionDibujada.lat, zoom)
        val piezas = modeloAmbulancia(posicionDibujada, rumboDibujado, largoM, destello).map { pieza ->
            val anillo = pieza.contorno.map { Point.fromLngLat(it.lon, it.lat) }
            // un poligono GeoJSON se cierra repitiendo el primer punto
            Feature.fromGeometry(Polygon.fromLngLats(listOf(anillo + anillo.first()))).apply {
                addStringProperty(PROP_COLOR, pieza.color)
                addNumberProperty(PROP_BASE, pieza.baseM)
                addNumberProperty(PROP_ALTO, pieza.altoM)
            }
        }
        fuente3d.setGeoJson(FeatureCollection.fromFeatures(piezas))
        fuente.setGeoJson(vacio)
    }

    // ---------------------------------------------------------- Ambulancia

    /**
     * Desliza la ambulancia hasta la nueva posicion en vez de teletransportarla,
     * girando por el lado mas corto (ver [interpolarRumbo]). Solo hay animacion
     * corriendo mientras hay movimiento: quieta, el mapa no gasta bateria.
     */
    private fun moverAmbulancia(destino: LatLon, rumboDestino: Float) {
        if (destino == posicionObjetivo && rumboDestino == rumboObjetivo) return
        posicionObjetivo = destino
        rumboObjetivo = rumboDestino
        animador?.cancel()
        animador = null

        val desde = posicionDibujada
        val rumboDesde = rumboDibujado
        val salto = distanciaLineaRectaM(desde, destino)

        if (mapa == null || salto > SALTO_SIN_ANIMAR_M) {
            posicionDibujada = destino
            rumboDibujado = rumboDestino
            pintarAmbulancia()
            if (modoNavegacion && siguiendoCamara) {
                camaraDeNavegacion(DURACION_CAMBIO_CAMARA_MS, suave = true)
            } else if (!modoNavegacion) {
                centrarSiQuedoFueraDeVista(destino)
            }
            return
        }

        animador = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = DURACION_MOVIMIENTO_MS.toLong()
            interpolator = LinearInterpolator()
            addUpdateListener { animacion ->
                val t = animacion.animatedValue as Float
                posicionDibujada = LatLon(
                    desde.lat + (destino.lat - desde.lat) * t,
                    desde.lon + (destino.lon - desde.lon) * t,
                )
                rumboDibujado = interpolarRumbo(rumboDesde, rumboDestino, t)
                pintarAmbulancia()
            }
            start()
        }
        // La camara viaja con la misma duracion y sin aceleracion, para ir
        // pegada a la ambulancia durante todo el tramo.
        if (modoNavegacion && siguiendoCamara) {
            camaraDeNavegacion(DURACION_MOVIMIENTO_MS, suave = false)
        }
    }

    // ---------------------------------------------------------- Camara

    /** Margen inferior tapado por la hoja de ruta, acotado para no comerse el mapa. */
    private fun margenInferior(): Int = margenInferiorPx.coerceIn(0, (mapView.height * 0.45f).toInt())

    /**
     * Camara tipo Google Maps: cerca, inclinada, mirando hacia donde avanza la
     * ambulancia. El margen superior empuja el centro hacia abajo, de modo que
     * la ambulancia queda en el tercio inferior y se ve mas calle por delante.
     */
    private fun camaraDeNavegacion(duracionMs: Int, suave: Boolean) {
        val mapa = mapa ?: return
        val abajo = margenInferior()
        val arriba = ((mapView.height - abajo) * 0.36f).toInt().coerceAtLeast(0)
        val posicion = CameraPosition.Builder()
            .target(posicionObjetivo.aLatLng())
            .zoom(ZOOM_NAVEGACION)
            .tilt(INCLINACION_NAVEGACION)
            .bearing(rumboObjetivo.toDouble())
            .padding(0.0, arriba.toDouble(), 0.0, abajo.toDouble())
            .build()
        mapa.easeCamera(CameraUpdateFactory.newCameraPosition(posicion), duracionMs, suave)
    }

    /** Vista de planificacion: plana, norte arriba, con la ruta completa a la vista. */
    private fun encuadrarRuta() {
        val mapa = mapa ?: return
        if (ruta.size < 2 || mapView.width == 0 || mapView.height == 0) return
        val limites = LatLngBounds.Builder()
            .includes(ruta.map { it.aLatLng() })
            .include(posicionObjetivo.aLatLng())
            .build()
        val costado = (48 * densidad).toInt()
        // arriba estan el buscador y los filtros; abajo, la hoja de ruta
        val arriba = (150 * densidad).toInt()
        val abajo = margenInferior() + (32 * densidad).toInt()
        mapa.easeCamera(
            CameraUpdateFactory.newLatLngBounds(limites, 0.0, 0.0, costado, arriba, costado, abajo),
            DURACION_CAMBIO_CAMARA_MS,
        )
    }

    private fun camaraPlana() {
        val mapa = mapa ?: return
        val posicion = CameraPosition.Builder(mapa.cameraPosition)
            .tilt(0.0)
            .bearing(0.0)
            .padding(0.0, 0.0, 0.0, 0.0)
            .build()
        mapa.easeCamera(CameraUpdateFactory.newCameraPosition(posicion), DURACION_CAMBIO_CAMARA_MS)
    }

    /** Tras un salto (ej. primer fix del GPS) la ambulancia puede quedar fuera de pantalla. */
    private fun centrarSiQuedoFueraDeVista(punto: LatLon) {
        val mapa = mapa ?: return
        val destino = punto.aLatLng()
        if (mapa.projection.visibleRegion.latLngBounds.contains(destino)) return
        mapa.easeCamera(CameraUpdateFactory.newLatLng(destino), DURACION_CAMBIO_CAMARA_MS)
    }
}

private fun LatLon.aLatLng() = LatLng(lat, lon)
