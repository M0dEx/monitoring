package eu.m0dex.monitoring

import eu.m0dex.monitoring.service.IServiceConfig
import io.ktor.http.*

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
    val additionalHeaders: Map<String, String> = emptyMap(),
    val expectedResponseRegex: Regex? = null,
    val intervalMillis: Long = 10000,
)

data class MonitoringServiceConfig(
    val monitored: Map<String, MonitoredServiceConfig>,
    val questDb: QuestDbConfig,
) : IServiceConfig