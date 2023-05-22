package eu.m0dex.monitoring.backend.gui.resources

import eu.m0dex.monitoring.backend.rest.resources.Services
import eu.m0dex.monitoring.database.schema.Status
import eu.m0dex.monitoring.monitor.MonitoredService
import io.ktor.resources.*
import io.ktor.server.freemarker.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.datetime.*
import kotlinx.serialization.Serializable
import org.ktorm.database.Database
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import java.time.Instant as JavaInstant

@Resource("/services")
class ServicesGui {
    @Serializable
    data class ServiceStatus(
        val description: Services.ServiceDescription,
        val status: Status,
        val uptime: Services.Service.Uptime.ServiceUptimeResponse,
        val history: List<Services.Service.Uptime.ServiceUptimeResponse>
    )

    companion object {
        private val HOUR_MILLIS = 1.hours.inWholeMilliseconds

        fun Instant.hourGroup(): Instant {
            return Instant.fromEpochMilliseconds(toEpochMilliseconds() / HOUR_MILLIS * HOUR_MILLIS)
        }
    }

    suspend fun response(database: Database, monitoredServices: Set<MonitoredService>): FreeMarkerContent = withContext(Dispatchers.Default) {
        val services = monitoredServices.map { monitoredService ->
            async {
                val services = Services()
                val service = Services.Service(parent = services, name = monitoredService.name)

                ServiceStatus(
                    description = monitoredService.description(),
                    status = service.response(database) ?: Status(online = false),
                    uptime = uptime(database, service),
                    history = history(database, service)
                )
            } 
        }.awaitAll()

        return@withContext FreeMarkerContent(
            "services.ftl",
            mapOf(
                "services" to services
            )
        )
    }

    private suspend fun uptime(database: Database, service: Services.Service): Services.Service.Uptime.ServiceUptimeResponse = withContext(Dispatchers.IO) {
        val uptime = Services.Service.Uptime(parent = service, from = Clock.System.now() - 1.days)

        return@withContext uptime.response(database)
    }

    private suspend fun history(
        database: Database,
        service: Services.Service
    ): List<Services.Service.Uptime.ServiceUptimeResponse> = withContext(Dispatchers.IO) {
        val now = Clock.System.now()
        val from = now - 23.hours

        val historyPerHour = Services.Service.History(parent = service, limit = 10000, from = from, to = now)
            .response(database)
            .groupBy { it.timestamp!!.hourGroup() }
            .toMutableMap()

        (from.hourGroup().toEpochMilliseconds() until now.hourGroup().toEpochMilliseconds() step HOUR_MILLIS)
            .map { Instant.fromEpochMilliseconds(it) }
            .map { historyPerHour.putIfAbsent(it, listOf()) }

        return@withContext historyPerHour
            .toSortedMap()
            .map {
                Services.Service.Uptime.ServiceUptimeResponse.fromCounts(
                    onlineCount = it.value.count { status -> status.online }.toLong(),
                    offlineCount = it.value.count { status -> !status.online }.toLong(),
                    from = it.key,
                    to = it.key + 1.hours
                )
            }
    }

    @Resource("{name}")
    class Service(val parent: ServicesGui = ServicesGui(), val name: String) {
        fun response(monitoredServices: Set<MonitoredService>): FreeMarkerContent? = monitoredServices
            .find { it.name == this.name }
            ?.description()
            ?.let {
                FreeMarkerContent(
                    "service/service.ftl",
                    mapOf(
                        "serviceDescription" to it
                    )
                )
            }

        @Resource("history")
        class History(val parent: Service, val from: Instant = JavaInstant.EPOCH.toKotlinInstant(), val to: Instant = Clock.System.now()) {
            suspend fun response(database: Database, monitoredServices: Set<MonitoredService>): FreeMarkerContent {
                val history = Services.Service.History(
                    parent = Services.Service(parent = Services(), name = parent.name),
                    from = from,
                    to = to
                ).response(database)

                return FreeMarkerContent(
                    "service/history.ftl",
                    mapOf(
                        "serviceDescription" to monitoredServices.find { it.name == parent.name }?.description(),
                        "serviceHistory" to history,
                        "from" to from,
                        "to" to to
                    )
                )
            }
        }
    }
}
