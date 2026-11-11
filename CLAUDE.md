# APIO — contexto para Claude Code

Proyecto individual de David (TECSUP, Diseño y Desarrollo de Software, IV ciclo). Plazo: diciembre de 2026.
README.md describe el sistema completo (backend FastAPI, GraphHopper, PWA React). Este archivo cubre lo que
hace falta para trabajar la **app Android** (`android/`) contra la rúbrica del curso.

## Objetivo inmediato

APIO es también el proyecto final del curso de Android con Jetpack Compose. El profe evalúa con una rúbrica
de 8 criterios (2.5 pts cada uno). Hay que cumplirlos en la app de `android/` sin tocar el backend: la app es
un cliente más del mismo FastAPI.

| Criterio | Estado | Notas |
|---|---|---|
| Git (+20 commits) | Cumple | Más de 20 commits; seguir con commits chicos y descriptivos |
| MVVM | Hecho | Tres ViewModels con `StateFlow`: `MapaViewModel` (ruta, trafico, IA, GPS), `HospitalesViewModel` (lista con reintento automatico cada 30 s si no vino del servidor) e `HistorialViewModel` (traslados de Room). Comparten un `ApioRepository` via `RepositorioHolder`; el repositorio depende de la interfaz `ApioApi` |
| IA integrada con datos reales | Hecho, con LLM real **y** heuristica | Dos capas: (1) `backend/app/services/ia.py` llama a Google Gemini (free tier, modelo `gemini-flash-lite-latest`) con el prompt armado desde ETA con/sin trafico real, avenidas en "alta" y privilegios cruzados; `POST /api/ia/recomendacion` devuelve el texto, cacheado 3 min por (destino, riesgo, ETA redondeado); se muestra en la tarjeta "✨ Recomendacion IA" de `PanelInfo`. (2) Heuristica propia on-device (`ia/EstimadorRiesgo.kt`): pondera el mismo delta de ETA, las avenidas en "alta" y el promedio de privilegios cruzados en un nivel bajo/medio/alto (`BannerRiesgo`) -- es la entrada que recibe el LLM *y* el respaldo que queda en pantalla si Gemini falla (sin key, sin red, cuota agotada): la app nunca muestra un error por eso, solo oculta la tarjeta |
| Datos (Room/Retrofit) | Hecho | Red con **Retrofit** (`androidApp/red/RetrofitApioApi.kt` + `ApioRetrofitService.kt`) detras de la interfaz comun `ApioApi`; cache de hospitales, ultima ruta e historial con **Room** (`androidApp/datos/room/` + `RoomRutaCacheStore.kt`), compilado con KSP 2.3.13. Ktor (`ApioApiClient`) queda solo para previews/iOS |
| Servicios | Hecho | `SeguimientoUbicacionServicio` (foreground service, `foregroundServiceType="location"`) + permiso de ubicacion en runtime; boton "GPS real" en pantalla lo prende/apaga |
| UI/UX Material 3 | Hecho | `ui/theme/Theme.kt` (`ApioTheme`, paleta propia con variante oscura) reemplaza el `MaterialTheme { }` por defecto; manifest ya no usa `Theme.Material.Light` sino `@style/Theme.Apio` (`androidApp/res/values[-night]/themes.xml`) |
| App sin crashes | Parcial | Hay estado `Error` con reintento y sondeo silencioso. Falta probar en dispositivo/emulador real con el backend corriendo y mas tests |
| Video (3–5 min) + README | Falta | `android/README.md` sigue siendo el de la plantilla de KMP; falta grabar el video (hay guion listo, ver GUION_VIDEO.md) |

### Nota sobre "Room/Retrofit"

Antes se uso `SQLiteOpenHelper` a mano porque se creyo que KSP no soportaba Kotlin 2.4.10. En realidad KSP2
(2.3.x) ya no va atado a la version de Kotlin y desde 2.3.1 soporta el "built-in Kotlin" de AGP 9, asi que se
migro a Room (KSP 2.3.13, Room 2.8.4) y la red Android a Retrofit 3. Los archivos de `SQLiteOpenHelper` ya se borraron.

## Orden de trabajo acordado

1. ~~Compilar y verificar el MVVM~~ Hecho.
2. ~~Datos (cache local + ultima ruta)~~ Hecho con Room + Retrofit (ver nota arriba).
3. ~~Tema Material 3 propio~~ Hecho.
4. ~~Servicio en primer plano + permiso de ubicación~~ Hecho.
5. ~~Definir e integrar el criterio de IA~~ Hecho: heuristica on-device + LLM real (Gemini) en el backend,
   endpoint probado en vivo (ver nota de Gemini abajo).
6. Probar en dispositivo/emulador real con el backend corriendo (`start.ps1`), grabar el video (3-5 min,
   guion en GUION_VIDEO.md) y terminar `android/README.md`.

### Nota sobre la API key de Gemini (para no repetir el mismo error)

`GEMINI_API_KEY` vive solo en `backend/.env` (gitignorado) y `backend/.env.example` SIEMPRE debe quedar con
`GEMINI_API_KEY=` vacio -- es un archivo versionado, poner ahi una key real la deja en el historial de git
para siempre. Una key real de Google AI Studio (https://aistudio.google.com/apikey, boton "Create API key")
es una sola cadena alfanumerica que empieza con `AIzaSy...` (~39 caracteres). Si lo que se copio empieza con
`AQ.` y tiene puntos/guiones, es un token de otro tipo (OAuth/sesion) y Gemini responde 401 con
`ACCESS_TOKEN_TYPE_UNSUPPORTED` -- hay que volver a la pagina y copiar el boton de la key, no otro campo.
El modelo default (`gemini_model` en `config.py` y `.env.example`) es `gemini-flash-lite-latest`: se probo que
`gemini-flash-latest` (el modelo "normal", no "lite") es valido pero devuelve 503 "high demand" seguido en el
free tier; el "lite" respondio 200 de forma consistente y alcanza de sobra para una recomendacion de 2
oraciones.

## Arquitectura de la app Android

- Kotlin Multiplatform + Compose Multiplatform. Módulos: `android/androidApp` (Activity) y `android/shared`.
- `shared/commonMain/.../pe/apio/mobile/`:
  - `modelo/` modelos serializables que calzan 1:1 con `/api/route` y `/api/traffic`, y la lista `HOSPITALES`.
  - `red/` `ApioApiClient` (Ktor). Base URL del emulador: `http://10.0.2.2:8000/api`.
  - `datos/` `ApioRepository` (capa de datos) + `RutaCacheStore` (interfaz de cache; la implementacion con
    SQLite vive en `androidApp`, ver nota de "Datos" arriba).
  - `ia/` `EstimadorRiesgo.kt`: heuristica on-device que clasifica el riesgo de demora de la ruta activa.
  - `viewmodel/` `MapaViewModel` y `MapaUiState` (flujo unidireccional con `StateFlow`).
  - `ui/` paneles Compose, `EstadoPantalla` (Vacio / Cargando / Exito / Error) y `ui/theme/` (`ApioTheme`).
  - `mapa/` `expect fun MapaOSM`; la implementación con osmdroid está en `androidMain`.
- `androidApp/src/main/kotlin/.../pe/apio/mobile/`:
  - `MainActivity.kt` arma el `ApioRepository` con cache SQLite y liga/desliga el foreground service de
    ubicacion (unico lugar que sabe de Android "de verdad"; `App()` en shared no conoce nada de esto).
  - `datos/` `ApioSqliteHelper` (SQLiteOpenHelper) + `SqliteRutaCacheStore` (implementa `RutaCacheStore`).
  - `servicio/` `SeguimientoUbicacionServicio` (foreground service de GPS real, "modo ambulancia real").
- `EstadoRuta` vive en `ui/` y el repositorio lo devuelve; si se quiere separar capas, moverlo a `modelo/`.

## Cómo trabajar aquí

- Idioma: español, tanto en la conversación como en comentarios y mensajes de commit.
- Estilo del repo: comentarios en español sin tildes dentro del código, mensajes de commit descriptivos
  con prefijo de Conventional Commits (`feat:`, `fix:`, `docs:`, `style:`, `refactor:`, `test:`, `chore:`)
  seguido del resumen en español (por ejemplo "feat: auto-refresh del ETA de la ruta activa").
- David prefiere resultados listos para usar (código completo, archivos ya editados) en vez de guías paso a paso.
- Los entregables del curso deben llevar cambios propios que se vean hechos por él; no copiar codelabs tal cual.
- Backend, GraphHopper y PWA no se tocan para esta entrega. Probar la app con el backend corriendo
  (`start.ps1` levanta todo el stack, `stop.ps1` lo detiene).
