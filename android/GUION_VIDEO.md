# Guion del video (3-5 min) — APIO Android

Guion de referencia para grabar el video del entregable. Grabar pantalla del emulador/dispositivo + voz,
con el backend corriendo (`./start.ps1` desde la raiz antes de empezar a grabar).

## 0. Antes de grabar

- Backend arriba y respondiendo (`http://localhost:8000/api/traffic` en el navegador, por ejemplo).
- App instalada y sin abrir todavia.
- Tener a la mano Android Studio con `CLAUDE.md` / el codigo para mostrar 2-3 fragmentos puntuales.

## 1. Intro (20-30s)

"Hola, soy David. Este es APIO, el cliente Android nativo de mi proyecto de enrutamiento para ambulancias
en Arequipa. Esta hecho en Kotlin Multiplatform con Compose, y habla con el mismo backend FastAPI que ya
usa la version web del proyecto — misma logica de privilegios de transito, mismo corredor piloto."

## 2. Demo funcional (90-120s)

1. Abrir la app → mostrar el mapa con los 4 hospitales marcados.
2. Tocar un hospital → mostrar el panel de carga → la ruta trazada + el panel de info (ETA tachado vs ETA
   con trafico, chips de privilegio cruzado).
3. Esperar o forzar un refresco de trafico (sondeo cada 25s) → senalar que el ETA se actualiza solo, sin
   recalcular la ruta completa ("se actualiza solo" con el punto verde pulsante).
4. Senalar el **banner de riesgo** (IA): explicar que no es el ETA del backend, es una heuristica propia de
   la app que combina 3 señales (demora relativa, avenidas en trafico alto, privilegios exigidos) en un
   nivel bajo/medio/alto.
5. Tocar el boton **"GPS real"** → aceptar el permiso de ubicacion → mostrar la notificacion persistente del
   foreground service y como el origen ahora sigue la ubicacion real (o la del emulador, si se simula GPS
   desde Android Studio).
6. Mover el mapa / tocar otro hospital con "GPS real" apagado para mostrar que el modo manual sigue
   funcionando.
7. (Opcional) Apagar el WiFi/backend un momento y volver a tocar el mismo hospital → mostrar que la app no
   se cae: usa la ultima ruta en cache (SQLite) en vez de romperse.

## 3. Arquitectura (60-90s), mostrando codigo

1. `viewmodel/MapaViewModel.kt` — senalar el `StateFlow<MapaUiState>` y como `App.kt` solo lo observa (MVVM).
2. `datos/ApioRepository.kt` — mostrar el try/catch que guarda en cache y cae a la cache si la red falla.
3. `androidApp/datos/ApioSqliteHelper.kt` — mencionar brevemente la decision tecnica: se intento Room con
   KSP/kapt, no hubo build compatible con el Kotlin de este proyecto, se opto por SQLiteOpenHelper nativo
   con la misma arquitectura (Entity/DAO/Database). (Ver CLAUDE.md / README para el detalle completo si el
   profesor pregunta.)
4. `servicio/SeguimientoUbicacionServicio.kt` — foreground service + permiso de ubicacion en runtime.
5. `ui/theme/Theme.kt` — tema Material 3 propio (`ApioTheme`), con variante oscura.
6. `ia/EstimadorRiesgo.kt` — la funcion `evaluarRiesgo`, explicando los 3 pesos.

## 4. Cierre (15-20s)

"Esto cubre los 8 criterios de la rubrica: MVVM, persistencia local, servicios, Material 3, la heuristica
de IA con datos reales de trafico, y mas de 20 commits en el repo. El codigo completo esta en GitHub."
(Mostrar el repo / `git log --oneline` corriendo.)

---

**Duracion objetivo:** 3-5 min. Si se alarga, recortar el paso 2.6 (modo manual) o el detalle de un solo
archivo de codigo en la seccion 3.
