package eu.m0dex.monitoring.monitor

import eu.m0dex.monitoring.service.ILoggable
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.resources.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.Instant

class MonitoredService(
    private val questDbChannel: Channel<StatusMessage>,
    val name: String,
    val displayName: String,
    val url: String,
    val method: HttpMethod,
    val intervalMillis: Long,
    val additionalHeaders: Map<String, String>,
    val expectedResponseRegex: Regex? = null,
) : ILoggable {
    private val httpClient = HttpClient(CIO) {
        install(WebSockets)
        install(Resources)
        defaultRequest {
            headers {
                additionalHeaders.map { (name, value) -> append(name, value) }
            }
        }
    }

    suspend fun checkAvailability() = coroutineScope {
        while (isActive) {
            var online = true
            var failReason: String? = null
            val now = Instant.now()

            val response = httpClient.request {
                url(this@MonitoredService.url)
                method = this@MonitoredService.method
            }

            expectedResponseRegex?.let {
                if (!it.matches(response.bodyAsText())) {
                    online = false
                    failReason = "Regex mismatch"
                }
            }

            val statusMessage = StatusMessage(
                online = online,
                serviceName = name,
                responseCode = response.status.value,
                latency = response.responseTime.timestamp - now.toEpochMilli(),
                failReason = failReason,
            )

            logger.info(statusMessage.toString())
            questDbChannel.send(statusMessage)

            delay(intervalMillis)
        }
    }
}