package eu.m0dex.monitoring.monitor

import eu.m0dex.monitoring.backend.rest.resources.Services
import eu.m0dex.monitoring.database.schema.Status
import eu.m0dex.monitoring.service.ILoggable
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.network.sockets.*
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.*
import io.ktor.client.plugins.resources.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class MonitoredService(
    private val questDbChannel: Channel<Pair<String, Status>>,
    val name: String,
    val displayName: String,
    val url: String,
    val method: HttpMethod,
    val intervalMillis: Long,
    val timeoutMillis: Long,
    val additionalHeaders: Map<String, String>,
    val expectedStatusCodes: Set<HttpStatusCode>,
    val expectedResponseRegex: Regex? = null,
    val payload: String? = null
) : ILoggable {
    private val httpClient = HttpClient(CIO) {
        install(Resources)
        install(HttpTimeout) {
            requestTimeoutMillis = timeoutMillis
            connectTimeoutMillis = timeoutMillis
            socketTimeoutMillis = timeoutMillis
        }
        defaultRequest {
            headers {
                additionalHeaders.map { (name, value) -> append(name, value) }
            }
        }
    }

    fun description() = Services.ServiceDescription(
        name = name,
        displayName = displayName,
        url = url,
        method = method.value,
        intervalMillis = intervalMillis
    )

    suspend fun checkAvailability() = coroutineScope {
        while (isActive) {
            var online = true
            var failReason: String? = null
            var response: HttpResponse? = null

            try {
                response = httpClient.request {
                    url(this@MonitoredService.url)
                    method = this@MonitoredService.method
                    payload?.let { setBody(it) }
                }
            } catch (ex: Exception) {
                when (ex) {
                    is HttpRequestTimeoutException,
                    is ConnectTimeoutException,
                    is SocketTimeoutException -> {
                        online = false
                        failReason = "Timed out"
                    }
                    else -> {
                        online = false
                        failReason = "An unknown exception occurred: ${ex.message}"
                    }
                }
            }

            response?.let { res ->
                expectedStatusCodes.find { it == res.status }.run {
                    if (this == null) {
                        online = false
                        failReason = "Unexpected response code"
                        return@let
                    }
                }

                expectedResponseRegex?.run {
                    if (!containsMatchIn(res.bodyAsText())) {
                        online = false
                        failReason = "Regex mismatch"
                        return@let
                    }
                }
            }

            val status = Status(
                online = online,
                responseCode = response?.run { status.value.toLong() },
                latency = response?.run { responseTime.timestamp - requestTime.timestamp },
                failReason = failReason
            )

            logger.info("Service '$name' status: $status")
            questDbChannel.send(Pair(name, status))

            delay(intervalMillis)
        }
    }
}
