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
| MVVM | Hecho y **compila** (`:androidApp:assembleDebug` OK) | `viewmodel/MapaViewModel.kt` + `datos/ApioRepository.kt`; `App.kt` solo observa `uiState` |
| IA integrada con datos reales | Hecho | Heuristica propia on-device (`ia/EstimadorRiesgo.kt`): pondera el delta ETA sin/con trafico real (TomTom), las avenidas en nivel "alta" que cruza la ruta y el promedio de privilegios cruzados, en un nivel bajo/medio/alto mostrado como banner en `PanelInfo` |
| Datos (Room/Retrofit) | Hecho, **sin Room literal** (ver nota) | Red con Ktor (`ApioApiClient`). Cache de hospitales y ultima ruta con `SQLiteOpenHelper` nativo (`androidApp/datos/ApioSqliteHelper.kt` + `SqliteRutaCacheStore.kt`), inyectado en `ApioRepository` como fallback cuando falla la red |
| Servicios | Hecho | `SeguimientoUbicacionServicio` (foreground service, `foregroundServiceType="location"`) + permiso de ubicacion en runtime; boton "GPS real" en pantalla lo prende/apaga |
| UI/UX Material 3 | Hecho | `ui/theme/Theme.kt` (`ApioTheme`, paleta propia con variante oscura) reemplaza el `MaterialTheme { }` por defecto; manifest ya no usa `Theme.Material.Light` sino `@style/Theme.Apio` (`androidApp/res/values[-night]/themes.xml`) |
| App sin crashes | Parcial | Hay estado `Error` con reintento y sondeo silencioso. Falta probar en dispositivo/emulador real con el backend corriendo y mas tests |
| Video (3–5 min) + README | Falta | `android/README.md` sigue siendo el de la plantilla de KMP; falta grabar el video (hay guion listo, ver GUION_VIDEO.md) |

### Nota sobre "Room/Retrofit": por que se uso SQLiteOpenHelper en vez de Room

Se intento agregar Room de verdad, pero Room necesita un procesador de anotaciones (KSP o kapt) y, a la
fecha de este entregable, ninguno de los dos es viable con el toolchain exacto de este proyecto:

- KSP no tiene ninguna build publicada para Kotlin 2.4.10 (verificado en Maven Central: la ultima version
  del plugin de KSP es 2.3.13).
- kapt (`org.jetbrains.kotlin.kapt`) es incompatible con el "built-in Kotlin" de AGP 9 que usa `:androidApp`
  (no aplica el plugin clasico de Kotlin, solo `com.android.application` + `composeCompiler`).

Downgradear el Kotlin del proyecto completo para habilitar KSP es riesgoso (Compose Multiplatform 1.11.1 /
AGP 9.0.1 estan acoplados a esta version) y hubiera puesto en riesgo lo que ya compila. La alternativa fue
reproducir a mano la misma arquitectura de Room -- Entity/DAO/Database -- sobre `SQLiteOpenHelper` (el mismo
motor SQLite que usa Room por debajo), documentado en el propio codigo. Si el profe exige el nombre "Room"
literal, decirlo para evaluar bajar la version de Kotlin/AGP del proyecto (riesgo: puede romper el build que
hoy funciona) o mostrar esta decision como parte del criterio de "Datos" (justificada con evidencia técnica).

## Orden de trabajo acordado

1. ~~Compilar y verificar el MVVM~~ Hecho.
2. ~~Datos (cache local + ultima ruta)~~ Hecho con SQLiteOpenHelper (ver nota arriba).
3. ~~Tema Material 3 propio~~ Hecho.
4. ~~Servicio en primer plano + permiso de ubicación~~ Hecho.
5. ~~Definir e integrar el criterio de IA~~ Hecho (heuristica on-device).
6. Probar en dispositivo/emulador real con el backend corriendo (`start.ps1`), grabar el video (3-5 min,
   guion en GUION_VIDEO.md) y terminar `android/README.md`.

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
  (por ejemplo "Paso 9 del cliente Android: auto-refresh del ETA de la ruta activa").
- David prefiere resultados listos para usar (código completo, archivos ya editados) en vez de guías paso a paso.
- Los entregables del curso deben llevar cambios propios que se vean hechos por él; no copiar codelabs tal cual.
- Backend, GraphHopper y PWA no se tocan para esta entrega. Probar la app con el backend corriendo
  (`start.ps1` levanta todo el stack, `stop.ps1` lo detiene).
