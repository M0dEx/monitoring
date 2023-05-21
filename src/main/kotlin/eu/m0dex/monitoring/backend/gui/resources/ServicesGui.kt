package eu.m0dex.monitoring.backend.gui.resources

import eu.m0dex.monitoring.backend.rest.resources.Services
import eu.m0dex.monitoring.monitor.MonitoredService
import eu.m0dex.monitoring.service.ILoggable
import io.ktor.resources.*
import kotlinx.datetime.*
import kotlinx.serialization.Serializable
import org.ktorm.database.Database
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

@Resource("/services")
class ServicesGui : ILoggable {
    @Serializable
    data class ServiceStatus(
        val description: Services.ServiceDescription,
        val uptime: Services.Service.Uptime.ServiceUptimeResponse,
        val history: List<Services.Service.Uptime.ServiceUptimeResponse>,
    )

    companion object {
        private val HOUR_MILLIS = 1.hours.inWholeMilliseconds

        fun Instant.hourGroup(): Instant {
            return Instant.fromEpochMilliseconds(toEpochMilliseconds() / HOUR_MILLIS * HOUR_MILLIS)
        }
    }

    suspend fun response(database: Database, monitoredServices: Set<MonitoredService>): List<ServiceStatus> {
        return monitoredServices.map { monitoredService ->
            val services = Services()
            val service = Services.Service(parent = services, name = monitoredService.name)

            ServiceStatus(
                description = monitoredService.description(),
                uptime = uptime(database, service),
                history = history(database, service),
            )
        }
    }

    private suspend fun uptime(database: Database, service: Services.Service): Services.Service.Uptime.ServiceUptimeResponse {
        val uptime = Services.Service.Uptime(parent = service, from = Clock.System.now() - 1.days)

        return uptime.response(database)
    }

    private suspend fun history(
        database: Database,
        service: Services.Service
    ): List<Services.Service.Uptime.ServiceUptimeResponse> {
        val now = Clock.System.now()
        val from = now - 23.hours

        val historyPerHour = Services.Service.History(parent = service, limit = 10000, from = from, to = now)
            .response(database)
            .groupBy { it.timestamp!!.hourGroup() }
            .toMutableMap()

        (from.hourGroup().toEpochMilliseconds() until now.hourGroup().toEpochMilliseconds() step HOUR_MILLIS)
            .map { Instant.fromEpochMilliseconds(it) }
            .map { historyPerHour.putIfAbsent(it, listOf()) }

        return historyPerHour
            .toSortedMap()
            .map {
                Services.Service.Uptime.ServiceUptimeResponse.fromCounts(
                    onlineCount = it.value.count { status -> status.online },
                    offlineCount = it.value.count { status -> !status.online },
                    from = it.key,
                    to = it.key + 1.hours,
                )
            }
    }
}