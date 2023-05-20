package eu.m0dex.monitoring

import eu.m0dex.monitoring.service.IServiceConfig
import io.ktor.http.*

data class ApiConfig(
    val bindAddress: String = "0.0.0.0",
    val bindPort: Int = 8080,
)

data class QuestDbConfig(
    val host: String,
    val writePort: Int = 9009,
    val readPort: Int = 8812,
    val username: String = "admin",
    val password: String = "quest",
    val writeBufferSize: Int = 64000,
)

data class MonitoredServiceConfig(
    val displayName: String,
    val url: String,
    val method: HttpMethod = HttpMethod.Head,
    val payload: String? = null,
    val additionalHeaders: Map<String, String> = emptyMap(),
    val expectedStatusCodes: Set<HttpStatusCode> = setOf(HttpStatusCode.OK),
    val expectedResponseRegex: Regex? = null,
    val intervalMillis: Long = 10000,
    val timeoutMillis: Long = 10000,
)

data class MonitoringServiceConfig(
    val api: ApiConfig,
    val questDb: QuestDbConfig,
    val monitored: Map<String, MonitoredServiceConfig>,
) : IServiceConfig