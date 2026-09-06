package pe.apio.mobile.red

import io.ktor.client.engine.HttpClientEngineFactory

// El resto de ApioApiClient es 100% comun; solo el motor de red concreto
// necesita ser especifico de plataforma (OkHttp en Android, Darwin si se
// agrega iOS despues).
expect fun motorHttpCliente(): HttpClientEngineFactory<*>
