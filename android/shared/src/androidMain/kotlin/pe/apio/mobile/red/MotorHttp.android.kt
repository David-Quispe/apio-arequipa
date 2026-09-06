package pe.apio.mobile.red

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.okhttp.OkHttp

actual fun motorHttpCliente(): HttpClientEngineFactory<*> = OkHttp
