# APIO — cliente Android nativo

App Android nativa de APIO, el sistema de enrutamiento para ambulancias en Arequipa. Esta hecha en Kotlin
Multiplatform + Compose Multiplatform y es un cliente mas del mismo backend FastAPI que usa la PWA: no
modifica el backend, GraphHopper ni el corredor de privilegios (contraflujo, carril exclusivo, cruce en rojo).

Este README es el entregable del curso de Android con Jetpack Compose (TECSUP, IV ciclo). Para el contexto
completo del proyecto (backend, GraphHopper, PWA) ver el `README.md` de la raiz del repo.

**Video de demostracion (3-5 min):** _pendiente de enlace_

## Stack

| Capa | Tecnologia |
|---|---|
| Lenguaje / UI | Kotlin 2.4.10, Compose Multiplatform 1.11.1, Material 3 |
| Arquitectura | MVVM + flujo unidireccional (`StateFlow`) |
| Red | Retrofit 3 + OkHttp + kotlinx.serialization |
| Persistencia local | Room 2.8.4 (con KSP 2.3.13) |
| Mapa | MapLibre Native Android 13.6 con estilos de MapTiler |
| Servicios | Foreground service de ubicacion + texto a voz del dispositivo |
| IA | Google Gemini (via backend) + heuristica de riesgo en la app |
| SDK | minSdk 24, targetSdk / compileSdk 36, AGP 9.0.1 |

## Como correrla

La app habla por defecto con el backend desplegado en Azure (NGINX + HTTPS de Let's Encrypt):
`https://apio-arequipa.duckdns.org/api`. Con eso basta un celular con datos o Wi-Fi.

Para trabajar contra el backend en tu PC, levanta el stack con `./start.ps1` (desde la raiz del repo) y cambia
`API_BASE_URL` en `shared/.../App.kt` por `http://10.0.2.2:8000/api` (alias del emulador hacia el `localhost`
del host). `network_security_config.xml` ya permite HTTP sin cifrar solo para esa direccion.

```bash
cd android
./gradlew :androidApp:assembleDebug
adb install androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

O abrir `android/` en Android Studio y ejecutar la configuracion `androidApp` en un emulador o dispositivo.

**Clave del mapa (opcional).** Para los estilos de MapTiler hace falta una clave gratuita de
https://cloud.maptiler.com en `android/local.properties` (el archivo esta en `.gitignore`, no se sube):

```
MAPTILER_KEY=tu_clave_aqui
```

Sin clave la app igual funciona: cae a un estilo libre de OpenFreeMap.

**Tests:** `./gradlew :shared:testAndroidHostTest` (repositorio y cache, `HospitalesViewModel`, heuristica de
riesgo, rumbo y progreso sobre la ruta, formateo y modelo de la ambulancia).

## Pantallas

Cuatro pestanas con `NavigationBar`, disenadas primero en Figma
([APIO - Diseno de vistas](https://www.figma.com/design/AU9vW9wMgnbdZkd7PazjX2)):

- **Mapa:** hospitales, ruta con ETA con y sin trafico, privilegios cruzados, banner de riesgo, tarjeta de
  recomendacion IA y modo navegacion.
- **Hospitales:** establecimientos de salud de Arequipa (RENIPRESS + OpenStreetMap) con busqueda por nombre y filtro por tipo.
- **Historial:** traslados anteriores, guardados en Room cada vez que se calcula una ruta hacia un hospital.
- **Perfil:** cantidad de traslados, tema claro/oscuro y encendido del GPS real.

## Arquitectura (MVVM)

```
shared/src/commonMain/kotlin/pe/apio/mobile/
├── modelo/      Modelos serializables 1:1 con /api/route y /api/traffic, y la lista de respaldo HOSPITALES
├── red/         Interfaz ApioApi (contrato de red) y ApioApiClient (Ktor, solo para previews)
├── datos/       ApioRepository (capa de datos) + RutaCacheStore (interfaz de la cache local)
├── ia/          EstimadorRiesgo — heuristica on-device de riesgo de demora
├── viewmodel/   MapaViewModel, HospitalesViewModel, HistorialViewModel + RepositorioHolder
├── ui/          Pantallas y paneles Compose, EstadoPantalla, ui/theme/ (ApioTheme)
├── mapa/        expect fun MapaApio(...), rumbo y progreso sobre la ruta
└── App.kt       La V del MVVM: observa los uiState y reenvia eventos

shared/src/androidMain/   Implementacion de MapLibre y del texto a voz

androidApp/src/main/kotlin/pe/apio/mobile/
├── MainActivity.kt   Unico lugar que arma lo "real" de Android (repositorio, permisos, servicio)
├── red/              ApioRetrofitService + RetrofitApioApi (implementa ApioApi)
├── datos/            RoomRutaCacheStore (implementa RutaCacheStore)
│   └── room/         ApioDatabase, Entidades, Daos, Convertidores
└── servicio/         SeguimientoUbicacionServicio (foreground service de GPS)
```

- **Modelo:** los modelos de `modelo/` y el `ApioRepository`, que decide de donde salen los datos.
- **Vista:** `App.kt` y las pantallas solo leen `uiState` y llaman a funciones del ViewModel.
- **ViewModel:** `MapaViewModel` (ruta, trafico, IA, GPS, llegada), `HospitalesViewModel` (lista con
  reintento automatico cada 30 s si no vino del servidor) e `HistorialViewModel` (traslados de Room).
  Los tres comparten un `ApioRepository` mediante `RepositorioHolder`, que sobrevive a las rotaciones.
- El repositorio depende de las interfaces `ApioApi` y `RutaCacheStore`, asi `shared` no sabe nada de
  Retrofit ni de Room y los tests usan implementaciones falsas (`Fakes.kt`).

## Mapeo contra la rubrica del curso

| # | Criterio | Donde esta |
|---|---|---|
| 1 | Control de versiones (Git, +20 commits) | Mas de 50 commits propios con mensajes descriptivos y prefijos (`feat:`, `fix:`, `refactor:`, `docs:`). El historial tambien incluye los commits automaticos del muestreo de trafico (`git log --author=David` los filtra) |
| 2 | Patron MVVM | `viewmodel/` (3 ViewModels con `StateFlow`) + `datos/ApioRepository.kt`; `App.kt` solo observa estado y envia eventos |
| 3 | IA integrada con datos reales | Recomendacion de Gemini via `POST /api/ia/recomendacion` + heuristica `ia/EstimadorRiesgo.kt`, ambas con trafico real de TomTom (ver "Criterio de IA") |
| 4 | Datos (Room/Retrofit) | Red con **Retrofit** (`androidApp/red/`), cache local con **Room** (`androidApp/datos/room/`): hospitales, ultima ruta por destino e historial de traslados |
| 5 | Servicios (Service de Android) | `servicio/SeguimientoUbicacionServicio.kt`: foreground service de ubicacion (`foregroundServiceType="location"`) con permiso en tiempo de ejecucion |
| 6 | UI/UX con Material 3 | `ApioTheme` con la paleta del Figma y modo oscuro, 4 pantallas con `NavigationBar`, iconos Material Symbols, modo navegacion con botones grandes y avisos que se cierran solos |
| 7 | App funcional sin errores criticos | Estado `EstadoPantalla.Error` con reintento, sondeo de trafico silencioso, cache cuando no hay red, y tests del repositorio y la logica de ruta |
| 8 | Documentacion + video (3-5 min) | Este README + video (enlace arriba) |

## Datos: Retrofit + Room

**Retrofit.** `ApioRetrofitService` declara los endpoints del backend (`route`, `traffic`, `establecimientos`,
`ia/recomendacion`) y `RetrofitApioApi` los expone como la interfaz `ApioApi`: convierte el JSON con
kotlinx.serialization, usa timeouts de 15 s (conexion) y 30 s (lectura, porque Gemini puede tardar) y traduce
los errores del backend (`{"detail": "..."}`) a `ApioApiException`.

**Room.** `ApioDatabase` (singleton) tiene tres tablas:

| Tabla | Entidad | Uso |
|---|---|---|
| `hospitales` | `HospitalEntity` | Establecimientos de salud para abrir la app sin red |
| `ultima_ruta` | `UltimaRutaEntity` | Ultima ruta calculada hacia cada destino |
| `traslados` | `TrasladoEntity` | Historial de traslados (pantallas Historial y Perfil) |

`RoomRutaCacheStore` implementa `RutaCacheStore`, y `ApioRepository` la usa como respaldo: si el servidor
falla, la lista de hospitales sale de Room y, si tampoco hay cache, de la lista incluida en la app
(`OrigenDatos.SERVIDOR`, `CACHE` o `RESPALDO`). La app nunca queda con la pantalla vacia por falta de red.

## Criterio de IA: recomendacion con LLM (Gemini) + heuristica de riesgo

La IA tiene dos capas que trabajan juntas:

1. **LLM real (Google Gemini, free tier).** Con una ruta activa, la app manda al backend
   (`POST /api/ia/recomendacion`, ver `backend/app/services/ia.py`) los datos reales de esa ruta: ETA con y
   sin trafico de TomTom, avenidas congestionadas que cruza, privilegios de transito disponibles y el nivel
   de riesgo de la heuristica. El backend arma el prompt, llama a Gemini y devuelve una recomendacion corta
   para el conductor, que se muestra en la tarjeta "Recomendacion IA" del panel.
   - La API key vive solo en `backend/.env` (`GEMINI_API_KEY`), nunca dentro del APK.
   - Para cuidar la cuota gratuita, la app solo vuelve a pedir la recomendacion cuando cambia el destino o
     el nivel de riesgo, y el backend reutiliza la misma respuesta 3 minutos para la misma situacion.
   - Si no hay key, no hay red o se agoto la cuota, el backend responde 503 y la app se queda con el
     mensaje de la heuristica local: la IA externa nunca tumba la pantalla.
2. **Heuristica on-device** (`ia/EstimadorRiesgo.kt`): es la entrada del LLM y su respaldo sin conexion.

### Heuristica on-device de riesgo de demora

El backend ya ajusta el ETA con trafico real, pero eso no es un componente de IA dentro de la app. Por eso
`EstimadorRiesgo.kt` calcula un puntaje de 0 a 1 como suma ponderada de tres señales que el backend
devuelve con datos reales:

- cuanto se alarga el ETA por trafico (peso 0.5),
- cuantas de las avenidas que cruza la ruta estan en nivel "alta" de trafico (peso 0.3),
- que tan exigentes son los privilegios cruzados — contraflujo, carril exclusivo, cruce en rojo — (peso 0.2).

El puntaje se clasifica en riesgo bajo (< 0.33), medio (< 0.66) o alto, con un mensaje, y se muestra como
banner de color. Es explicable punto por punto (no es una caja negra) y se recalcula cada vez que llega
trafico nuevo (cada 25 s) o se traza una ruta nueva.

## Servicio en primer plano + ubicacion real

Por defecto el origen de la ambulancia se mueve tocando el mapa (util para demos sin depender del GPS del
emulador). El boton **"GPS real"** prende `SeguimientoUbicacionServicio`: un foreground service que pide el
permiso de ubicacion en tiempo de ejecucion y actualiza el origen con la ubicacion real del dispositivo
cada ~1 s / 2 m (con rumbo y velocidad cuando el GPS los informa), mientras la notificacion persistente esta
visible. Si la posicion nueva viene con mucho peor precision que la anterior, se descarta.

## Mapa y modo navegacion (MapLibre + MapTiler)

El mapa usa **MapLibre Native Android** (vectorial, se puede inclinar y rotar) con estilos de **MapTiler**
(`streets-v2` de dia, `streets-v2-dark` con el tema oscuro). Reemplazo a osmdroid, que era solo 2D.

Con una ruta calculada y el GPS real prendido ("Iniciar ruta"), la pantalla entra en **modo navegacion**:

- camara inclinada (55 grados) que sigue a la ambulancia y gira segun su rumbo;
- ambulancia en 3D que rota con el rumbo, se desliza entre lecturas del GPS y no gira estando detenida
  (`mapa/Rumbo.kt`, con tests);
- hoja inferior compacta con el ETA grande y el boton **Terminar**; el detalle se ve al expandirla;
- boton **Recentrar** si se arrastra el mapa, avisos de cambio de riesgo que se cierran solos y avisos
  hablados con el texto a voz del dispositivo;
- al llegar a pocos metros del hospital el traslado se cierra solo y se avisa la llegada.

Todo el codigo de MapLibre esta en `shared/src/androidMain/.../mapa/` (`MapaApio.android.kt` maneja el
ciclo de vida del `MapView`; `ControladorMapa.android.kt`, el estilo, las capas y la camara).
