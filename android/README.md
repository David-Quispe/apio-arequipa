# APIO — cliente Android nativo

App Android nativa de APIO (enrutamiento para ambulancias en Arequipa), hecha en Kotlin Multiplatform +
Compose Multiplatform. Es un cliente mas del mismo backend FastAPI que ya usan la PWA y la demo — no
modifica el backend, GraphHopper ni el corredor de privilegios.

Este README es el entregable del curso de Android con Jetpack Compose (TECSUP, IV ciclo). Para el contexto
completo del proyecto APIO (backend, GraphHopper, PWA) ver el `README.md` de la raiz del repo.

## Como correrla

1. Levantar el backend real: `./start.ps1` desde la raiz del repo (o `docker compose up` + GraphHopper segun
   el README principal). La app **no tiene modo demo propio**: siempre habla con
   `http://10.0.2.2:8000/api`, el alias que usa el emulador Android para llegar al `localhost` del host.
2. Compilar e instalar:
   ```bash
   cd android
   ./gradlew :androidApp:assembleDebug
   adb install androidApp/build/outputs/apk/debug/androidApp-debug.apk
   ```
   O abrir `android/` en Android Studio y correr la configuracion `androidApp` sobre un emulador/dispositivo
   con API 24+.
3. Tests: `./gradlew :shared:testAndroidHostTest` (modelos, formateo, heuristica de riesgo).

## Arquitectura (MVVM)

```
shared/src/commonMain/kotlin/pe/apio/mobile/
├── modelo/      Modelos serializables 1:1 con /api/route y /api/traffic, y HOSPITALES
├── red/         ApioApiClient (Ktor) — mismo contrato que la PWA
├── datos/       ApioRepository (capa de datos) + RutaCacheStore (puerto de cache)
├── ia/          EstimadorRiesgo — heuristica on-device de riesgo de demora
├── viewmodel/   MapaViewModel + MapaUiState (StateFlow, flujo unidireccional)
├── ui/          Paneles Compose, EstadoPantalla, ui/theme/ (ApioTheme)
├── mapa/        expect fun MapaApio(...) — implementacion con MapLibre en androidMain
└── App.kt       La V del MVVM: solo observa uiState y reenvia eventos

androidApp/src/main/kotlin/pe/apio/mobile/
├── MainActivity.kt   Unico lugar que sabe de Android "de verdad" (ver mas abajo)
├── datos/            ApioSqliteHelper + SqliteRutaCacheStore (implementa RutaCacheStore)
└── servicio/         SeguimientoUbicacionServicio (foreground service de GPS real)
```

`App()` (la Vista) nunca crea un `ApioRepository` con detalles de Android: recibe una fabrica
(`crearRepositorio: () -> ApioRepository`) y, opcionalmente, un `Flow` de ubicacion real. Quien sabe de
Android — permisos, `Service`, SQLite — es `MainActivity`. Esto es lo que permite que `shared` compile para
mas plataformas despues sin arrastrar nada especifico de Android.

## Mapeo contra la rubrica del curso

| # | Criterio | Donde esta |
|---|---|---|
| 1 | Control de versiones (Git, +20 commits) | Historial del repo completo; commits chicos y descriptivos |
| 2 | Patron MVVM | `viewmodel/MapaViewModel.kt` + `datos/ApioRepository.kt`; `App.kt` solo observa `uiState` |
| 3 | IA integrada con datos reales | LLM Google Gemini via `POST /api/ia/recomendacion` (backend) + `ia/EstimadorRiesgo.kt` — ver seccion "Criterio de IA" abajo |
| 4 | Persistencia de datos (Room/Retrofit) | Red: Ktor (`red/ApioApiClient.kt`). Persistencia: `SQLiteOpenHelper` nativo (`androidApp/datos/`): cache de hospitales, ultima ruta e **historial de traslados** (tabla `traslados`, pantalla Historial) — ver nota sobre Room abajo |
| 5 | Servicios (Service de Android) | `servicio/SeguimientoUbicacionServicio.kt` — foreground service de ubicacion real |
| 6 | UI/UX con Material 3 | Rediseno basado en el Figma propio ([APIO - Diseno de vistas](https://www.figma.com/design/AU9vW9wMgnbdZkd7PazjX2)): 4 pantallas (`ui/pantallas/`: Mapa, Hospitales, Historial, Perfil) con `NavigationBar`, hoja de ruta inferior (`ui/PanelInfo.kt`), iconos Material Symbols (`ui/IconosApio.kt`) y `ApioTheme` con la paleta del Figma y modo oscuro conmutable desde Perfil |
| 7 | App funcional sin errores criticos | `EstadoPantalla.Error` con reintento; sondeo y auto-refresh silenciosos (no tiran la UI abajo) |
| 8 | Documentacion + video (3-5 min) | Este README + `GUION_VIDEO.md` |

### Criterio de IA: recomendacion con LLM (Gemini) + heuristica de riesgo

La IA tiene dos capas que trabajan juntas:

1. **LLM real (Google Gemini, free tier).** Cuando hay una ruta activa, la app manda al backend
   (`POST /api/ia/recomendacion`, ver `backend/app/services/ia.py`) los datos reales de esa ruta: ETA con y
   sin trafico de TomTom, avenidas congestionadas que cruza, privilegios de transito disponibles y el nivel
   de riesgo de la heuristica. El backend arma el prompt, llama a Gemini y devuelve una recomendacion corta
   para el conductor, que se muestra en la tarjeta "✨ Recomendacion IA" del panel.
   - La API key vive solo en `backend/.env` (`GEMINI_API_KEY`), nunca dentro del APK.
   - Para cuidar la cuota gratuita, la app solo vuelve a pedir la recomendacion cuando cambia el destino o
     el nivel de riesgo, y el backend reusa la misma respuesta 3 minutos para la misma situacion.
   - Si no hay key, no hay red o se agoto la cuota, el backend responde 503 y la app se queda con el
     mensaje de la heuristica local: la IA externa nunca tumba la pantalla.
2. **Heuristica on-device** (la señal de entrada del LLM y su respaldo offline), descrita abajo.

#### Heuristica on-device de riesgo de demora

El backend ya ajusta el ETA con trafico real (TomTom, ver `backend/app/services/traffic.py`), pero eso no
es un componente de IA *en la app*. Para cumplir el criterio con datos reales (no un mock), `ia/EstimadorRiesgo.kt`
implementa una heuristica propia — un arbol de decision ponderado, no una caja negra — que combina 3 señales
que el backend ya devuelve con datos de trafico real:

- cuanto se alarga el ETA por trafico (peso 0.5),
- cuantas de las avenidas que cruza la ruta estan en nivel "alta" de trafico (peso 0.3),
- que tan exigentes son los privilegios cruzados que necesita la ambulancia — contraflujo, carril
  exclusivo, cruce en rojo — (peso 0.2).

El resultado es un nivel bajo/medio/alto con un mensaje, mostrado como un banner de color en el panel de
info cuando hay una ruta activa. Se recalcula solo, cada vez que llega trafico nuevo (cada 25s) o se traza
una ruta nueva.

### Nota sobre "Datos (Room/Retrofit)": por que no hay Room literal

Se intento usar Room de verdad antes que nada. Room necesita un procesador de anotaciones (KSP o kapt), y
ninguno de los dos es viable con el toolchain exacto de este proyecto (Kotlin 2.4.10, AGP 9.0.1):

- **KSP** no tiene ninguna build publicada para Kotlin 2.4.10 (verificado en Maven Central — la ultima
  version del plugin es 2.3.13).
- **kapt** es incompatible con el "built-in Kotlin" de AGP 9 que usa `:androidApp` (el modulo ya no aplica
  el plugin clasico de Kotlin, solo `com.android.application` + el compilador de Compose).

Bajar la version de Kotlin del proyecto entero para habilitar KSP arriesgaba romper lo que ya compilaba
(Compose Multiplatform 1.11.1 y AGP 9.0.1 estan acoplados a esta version de Kotlin). En su lugar,
`androidApp/datos/ApioSqliteHelper.kt` + `SqliteRutaCacheStore.kt` reproducen a mano la misma arquitectura
de Room — Entity/DAO/Database — sobre `SQLiteOpenHelper` (el mismo motor SQLite que usa Room por debajo):
cachea la lista de hospitales y la ultima ruta calculada, y `ApioRepository` la usa como respaldo cuando la
red falla. Documentado tambien en `CLAUDE.md` para que quede clara la decision y su justificacion tecnica.

### Servicio en primer plano + ubicacion real

Por defecto, el origen de la ambulancia se mueve tocando el mapa (util para grabar la demo sin depender del
GPS del emulador). El boton **"GPS real"** prende `SeguimientoUbicacionServicio`: un foreground service
(`foregroundServiceType="location"`) que pide el permiso de ubicacion en tiempo de ejecucion y, una vez
concedido, actualiza el origen con la ubicacion real del dispositivo cada ~1s/2m (con rumbo y velocidad
cuando el GPS los informa) mientras la notificacion persistente esta visible.

### Mapa y modo navegacion (MapLibre + MapTiler)

El mapa usa **MapLibre Native Android** (vectorial, se puede inclinar y rotar) con estilos de **MapTiler**
(`streets-v2` de dia, `streets-v2-dark` con el tema oscuro). Reemplaza a osmdroid, que era solo 2D.

Para compilar hace falta una clave gratuita de https://cloud.maptiler.com en `android/local.properties`
(el archivo esta en `.gitignore`, la clave nunca se sube):

```
MAPTILER_KEY=tu_clave_aqui
```

Sin clave la app igual funciona: cae a un estilo libre de OpenFreeMap.

Con una ruta calculada y el GPS real prendido ("Iniciar ruta"), la pantalla entra en **modo navegacion**:

- camara inclinada (55 grados) que sigue a la ambulancia y gira segun su rumbo;
- ambulancia "falso 3D": un sprite propio que rota con el rumbo, se desliza entre lecturas del GPS y no
  gira estando detenida (`mapa/Rumbo.kt`, con tests);
- hoja inferior compacta con el ETA grande y el boton **Terminar**; el detalle se ve al expandirla;
- boton **Recentrar** si se arrastra el mapa, y avisos de cambio de riesgo que se cierran solos.

Todo el codigo de MapLibre esta en `shared/src/androidMain/.../mapa/` (`MapaApio.android.kt` maneja el
ciclo de vida del `MapView`; `ControladorMapa.android.kt` el estilo, las capas y la camara).
