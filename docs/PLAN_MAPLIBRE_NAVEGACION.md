# Plan: vista de navegación tipo Google Maps en APIO (MapLibre + ambulancia "falso 3D")

> Documento para empezar una conversación nueva. Pégalo completo (o adjúntalo) y di:
> **"Implementa este plan en mi proyecto APIO."**

---

## 1. Contexto del proyecto

- **Proyecto:** APIO — ruteo óptimo para ambulancias en Arequipa (proyecto individual, TECSUP, Diseño y Desarrollo de Software, IV ciclo). También es el proyecto final del curso de Android.
- **Carpeta local:** `C:\Users\David\Documents\Apio\Proyecto spoth`
- **Repo:** https://github.com/David-Quispe/apio-arequipa (rama `master`)
- **App Android:** `android/` — Kotlin Multiplatform + Compose Multiplatform.
  - `android/androidApp` → `MainActivity`, Room (`datos/room/`), Retrofit (`red/`), servicio GPS (`servicio/SeguimientoUbicacionServicio.kt`).
  - `android/shared/src/commonMain/kotlin/pe/apio/mobile/` → `App.kt`, ViewModels (`viewmodel/MapaViewModel.kt`, `HospitalesViewModel.kt`, `HistorialViewModel.kt`), pantallas (`ui/pantallas/`), modelos (`modelo/`).
  - **El mapa:** `expect fun MapaOSM(...)` en `shared/src/commonMain/.../mapa/` y su implementación con **osmdroid** en `shared/src/androidMain/kotlin/pe/apio/mobile/mapa/MapaOSM.android.kt`.
- **Toolchain (no cambiar):** Kotlin 2.4.10, AGP 9.0.1 (built-in Kotlin), Compose Multiplatform 1.11.1, minSdk 24, compileSdk 36, KSP 2.3.13, Room 2.8.4, Retrofit 3.0.0.
- **Backend:** FastAPI + GraphHopper + PostGIS en una VM de Azure, `https://apio-arequipa.duckdns.org/api`. **Este cambio no toca el backend** (el mapa lo dibuja el celular).
- **Estilo del repo:** comentarios en español **sin tildes** dentro del código; commits descriptivos en español; David prefiere que le entreguen los archivos ya editados, no guías paso a paso.

### Estado actual de la app (ya funciona)
- MVVM con 3 ViewModels y `StateFlow`; Room + Retrofit; servicio en primer plano de GPS en tiempo real (GPS + red).
- `MapaViewModel.actualizarUbicacionReal(LatLon)` mueve la ambulancia (origen) y recalcula la ruta cada 40 m **sin borrarla**.
- El mapa muestra: marcador de la ambulancia (origen), marcadores de hospitales (color por tipo; los sin emergencia en gris y más chicos), y la polilínea de la ruta. Tap en el mapa = mover origen; tap en hospital = calcular ruta.
- Rúbrica del curso (todo cubierto excepto video + README): Git, MVVM, IA, Room/Retrofit, Servicios, Material 3, sin crashes.

---

## 2. Objetivo

Que al **iniciar una ruta** la app se vea como **Google Maps en modo navegación**:

1. **Cámara inclinada (pitch ~50–60°)** que **sigue a la ambulancia** y **gira según su rumbo** (bearing).
2. **Ambulancia "falso 3D"**: una imagen de ambulancia en perspectiva que **rota** según la dirección de movimiento y se **desliza suave** entre posiciones GPS (no salta).
3. **Ruta destacada** (línea gruesa con borde), el tramo ya recorrido opcionalmente más tenue.
4. **Modo conducción** (UI): al haber ruta activa, el panel inferior se compacta a una franja con **ETA grande**, distancia, hospital y **un botón grande "Terminar"**; deslizando hacia arriba se ve el detalle (privilegios, tráfico, IA).
5. **Botón "Recentrar"** cuando el usuario mueve el mapa con el dedo (como Google Maps).
6. **Mapa oscuro de noche** (estilo oscuro automático según hora o según el tema de la app).

Fuera de alcance: modelo 3D real (glTF), voz/TTS (puede ser el siguiente paso), cambios al backend.

---

## 3. Decisiones ya tomadas

| Tema | Decisión | Por qué |
|---|---|---|
| Librería de mapa | **MapLibre Native Android** (`org.maplibre.gl:android-sdk`, rama 13.x u 12.x estable) reemplaza a osmdroid | osmdroid es solo 2D: no inclina cámara ni gira bien |
| Teselas | **MapTiler** (plan gratis) con estilo vectorial `streets-v2` y `streets-v2-dark` | Necesario para mapa vectorial con inclinación |
| Ambulancia | **Sprite "falso 3D"** (PNG/vector en perspectiva) con `icon-rotate` = rumbo | Se ve bien, fluido en gama baja, sin cargar el server |
| Servidor | Sin cambios | Todo es render en el celular |
| Arquitectura | Mantener `expect fun MapaOSM(...)` (o renombrar a `MapaApio`) en commonMain; solo cambia la implementación `androidMain` | No romper MVVM ni las pantallas |

---

## 4. Lo que David tiene que hacer antes (5 min)

1. Crear cuenta gratis en **https://cloud.maptiler.com** → *API Keys* → copiar la clave.
2. Poner la clave en `android/local.properties` (NO subirla a git):
   ```
   MAPTILER_KEY=tu_clave_aqui
   ```
3. (Recomendado) En MapTiler, restringir la clave al paquete Android `pe.apio.mobile`.

La clave se expone a la app vía `BuildConfig` (o `manifestPlaceholders`/recurso generado) en `androidApp/build.gradle.kts`, y se pasa al módulo shared (por ejemplo como parámetro de `App(...)` o en un `object ConfigMapa` en androidMain).

---

## 5. Pasos de implementación

### Paso 0 — Verificar versiones
- Confirmar en Maven Central la última versión **estable** de `org.maplibre.gl:android-sdk` (13.x u 12.x). No usar `-pre`.
- Usar la variante por defecto `android-sdk` (OpenGL ES); **no** Vulkan.
- Revisar que no choque con Kotlin 2.4.10 / AGP 9 (es una AAR normal, sin procesadores).

### Paso 1 — Dependencias
- `gradle/libs.versions.toml`: agregar `maplibre = "<version>"` y `maplibre-android = { module = "org.maplibre.gl:android-sdk", version.ref = "maplibre" }`.
- `shared/build.gradle.kts` → `androidMain.dependencies`: agregar `implementation(libs.maplibre.android)`. Dejar osmdroid **hasta que todo funcione**; quitarlo al final.
- `androidApp/build.gradle.kts`: leer `MAPTILER_KEY` de `local.properties` y exponerla (`buildConfigField` con `buildFeatures { buildConfig = true }`).

### Paso 2 — Inicialización
- Llamar `MapLibre.getInstance(context)` una vez (en `MainActivity.onCreate` o en el composable con `remember`).
- URL de estilo:
  - Día: `https://api.maptiler.com/maps/streets-v2/style.json?key=$KEY`
  - Noche: `https://api.maptiler.com/maps/streets-v2-dark/style.json?key=$KEY`

### Paso 3 — Nuevo `MapaOSM.android.kt` con MapLibre (misma firma `actual fun`)
Reimplementar con `AndroidView { MapView(context) }`, cuidando el ciclo de vida (`onStart/onResume/onPause/onStop/onDestroy` vía `LifecycleEventObserver` de `LocalLifecycleOwner`).

Capas (usar **sources/layers de estilo**, no la API vieja de `Marker`):
- `GeoJsonSource("ruta")` + `LineLayer` doble: borde (más ancho, color oscuro) y línea principal (color primario M3), `lineCap/lineJoin = ROUND`.
- `GeoJsonSource("hospitales")` + `SymbolLayer` con íconos por tipo (registrar los bitmaps en el estilo con `style.addImage("hosp_MINSA", bitmap)` etc.). Hospitales sin emergencia: ícono gris más chico (`iconSize` por propiedad). Mantener el filtro "Con emergencia / Todos".
- `GeoJsonSource("ambulancia")` + `SymbolLayer` con `iconImage("ambulancia")`, `iconRotate(get("rumbo"))`, `iconRotationAlignment(MAP)`, `iconPitchAlignment(MAP)` (para que se "acueste" con la inclinación y parezca 3D), `iconAllowOverlap(true)`.
- Clicks: `map.addOnMapClickListener` → `queryRenderedFeatures` sobre la capa de hospitales; si toca un hospital → `onTapHospital`, si no → `onTapMapa(LatLon)`.
- Actualizar sources en el bloque `update` de `AndroidView` (con `setGeoJson`), **sin recrear el MapView**.

### Paso 4 — Ambulancia animada y con rumbo
- Calcular rumbo (bearing) entre la posición anterior y la nueva (fórmula de azimut). Si el GPS da `Location.bearing` con `hasBearing()`, preferirlo: considerar exponer `rumbo` desde `SeguimientoUbicacionServicio` junto a `LatLon` (por ejemplo un `data class PosicionGps(val punto: LatLon, val rumbo: Float?)`), sin romper `actualizarUbicacionReal`.
- Interpolar posición y rumbo en ~800 ms con `ValueAnimator` (o `Animatable` de Compose) y actualizar el `GeoJsonSource("ambulancia")` en cada frame. Rotar por el camino más corto (manejar el salto 359°→0°).
- Ignorar cambios de rumbo cuando la velocidad es casi 0 (para que no gire solo estando quieto).

### Paso 5 — Cámara de navegación
- Nuevo estado de UI en `MapaUiState` o parámetro del mapa: `modoNavegacion: Boolean` (true cuando `estado is EstadoPantalla.Exito` **y** el seguimiento GPS está activo) y `siguiendoCamara: Boolean`.
- En modo navegación: `CameraPosition(target = ambulancia, zoom = 17.0, tilt = 55.0, bearing = rumbo)` con `easeCamera(..., 800ms)`; agregar `padding` superior para que la ambulancia quede en el **tercio inferior** de la pantalla (como Google Maps).
- Si el usuario arrastra el mapa (`addOnCameraMoveStartedListener` con `REASON_API_GESTURE`) → `siguiendoCamara = false` y mostrar botón **"Recentrar"** (FAB grande). Al tocarlo vuelve a seguir.
- Fuera de navegación: cámara plana (tilt 0, bearing 0) y encuadre de la ruta completa (`LatLngBounds` con padding) al calcularla.

### Paso 6 — Modo conducción en la UI (Compose, commonMain)
- En `PantallaMapa.kt` / `PanelInfo.kt`: cuando `modoNavegacion`:
  - Franja inferior compacta: **ETA grande** (`displaySmall`/`headlineLarge`), "· X km", nombre del hospital, botón **"Terminar"** de al menos 56–64 dp.
  - Ocultar buscador y chips de filtro (menos distracción).
  - El detalle (privilegios, tráfico, recomendación IA) queda en la hoja expandible.
- Alertas temporales arriba (Snackbar o card) cuando el riesgo pasa a ALTO o cambia el tráfico, que se cierran solas a los ~5 s y **no tapan la ruta**.
- Botones tocables manejando: mínimo 56 dp (ideal 64–76 dp).

### Paso 7 — Mapa oscuro
- Elegir estilo día/noche según el tema de la app (`temaOscuro` ya existe en `App.kt`) o la hora (19:00–06:00). Al cambiar, `map.setStyle(...)` y **volver a agregar** imágenes/sources/layers en el callback `onStyleLoaded`.

### Paso 8 — Asset de la ambulancia
- Crear `androidApp/src/main/res/drawable/ambulancia_3d.png` (o vector) vista desde arriba-atrás en perspectiva, apuntando "hacia arriba" (norte = 0°), ~96×96 px @xxhdpi, fondo transparente. Debe ser **original** (no copiar íconos con marca). Si no hay imagen, generar un vector simple propio (cuerpo blanco, franja roja, cruz, luces azules) con `VectorDrawable`.

### Paso 9 — Limpieza
- Cuando todo funcione: quitar osmdroid de `libs.versions.toml` y `shared/build.gradle.kts`, borrar el código viejo y la configuración de User-Agent de osmdroid.
- Renombrar `MapaOSM` → `MapaApio` (opcional, cuidando imports).
- Actualizar `CLAUDE.md` (criterio UI/UX) y la sección Android del `README.md`.
- Commits separados: (1) dependencia + clave MapTiler, (2) mapa MapLibre con capas, (3) ambulancia animada + cámara de navegación, (4) modo conducción en la UI, (5) mapa oscuro + limpieza de osmdroid.

---

## 6. Criterios de aceptación (probar en el celular, con datos móviles)

- [ ] El mapa carga con estilo MapTiler; se ven hospitales y filtro "Con emergencia / Todos" funciona.
- [ ] Tap en hospital calcula ruta; tap en el mapa mueve el origen (igual que antes).
- [ ] Con seguimiento GPS + ruta activa: cámara inclinada, sigue a la ambulancia y gira con el rumbo.
- [ ] La ambulancia se desliza suave y rota según la dirección; quieta no gira sola.
- [ ] Al arrastrar el mapa aparece "Recentrar" y funciona.
- [ ] Panel compacto con ETA grande y botón "Terminar"; el detalle se ve al expandir.
- [ ] Modo oscuro cambia el estilo del mapa sin perder capas.
- [ ] Rotar pantalla / minimizar y volver no crashea ni pierde la ruta (ciclo de vida del MapView).
- [ ] Los tests existentes siguen pasando (`./gradlew :shared:allTests` o *Run Tests* en `commonTest`).
- [ ] No se subió la clave de MapTiler a git (`local.properties` está en `.gitignore`).

---

## 7. Riesgos y cómo manejarlos

| Riesgo | Mitigación |
|---|---|
| Versión de MapLibre incompatible / crash nativo en el celular | Probar primero un mapa básico (Paso 2–3 sin capas). Si falla, bajar a la 12.x estable. |
| Clave de MapTiler expuesta | `local.properties` + restricción por paquete en MapTiler. |
| Cupo gratis de MapTiler | Para un proyecto de curso sobra; el mapa cachea teselas. |
| Perder funciones de osmdroid (clicks, colores) | Mantener osmdroid hasta terminar y comparar lado a lado. |
| Batería / rendimiento | Sprite (no glTF), animación solo cuando hay movimiento, OpenGL ES (no Vulkan). |
| `setStyle` borra capas | Reconstruir todo en `onStyleLoaded`. |

---

## 8. Pendientes generales del proyecto (por si sirven de contexto)

- README de la app (instalación, arquitectura, Azure, capturas, link del video) y **video de 3–5 min** para la rúbrica.
- Cambiar la clave SSH del servidor de Azure (la anterior quedó expuesta en un chat).
- La prueba gratuita de Azure vence ~**10 de diciembre de 2026**: decidir a dónde mover el backend antes de esa fecha.
- Hay cambios sin commit de otra sesión (README.md, backend `route.py`/`traffic.py`/`corridor_overlap.py`/`load_osm_graph.py`, frontend, `routing/apio_car.json`, `start.ps1`) que David debe revisar; no incluirlos en los commits de este plan.
