package eu.m0dex.monitoring.backend.rest.resources

import eu.m0dex.monitoring.monitor.MonitoredService
import eu.m0dex.monitoring.service.ILoggable
import eu.m0dex.monitoring.database.schema.Status
import eu.m0dex.monitoring.database.schema.StatusTable
import io.ktor.resources.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import kotlinx.datetime.toKotlinInstant
import kotlinx.serialization.Serializable
import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.postgresql.util.PSQLException
import java.time.Instant as JavaInstant

@Resource("/api/services")
class Services {
    @Serializable
    data class ServiceDescription(
        val name: String,
        val displayName: String,
        val url: String,
        val method: String,
        val intervalMillis: Long,
    )

    fun response(monitoredServices: Set<MonitoredService>): List<ServiceDescription> {
        return monitoredServices.map { it.description() }
    }

    @Resource("{name}")
    class Service(val parent: Services = Services(), val name: String, val closestBefore: Instant = Instant.DISTANT_FUTURE) : ILoggable {
        suspend fun response(database: Database): Status? = withContext(Dispatchers.IO) {
            val table = StatusTable(name)

            return@withContext try {
                database
                    .from(table)
                    .select()
                    .where(table.timestamp.lessEq(closestBefore.toJavaInstant()))
                    .orderBy(table.timestamp.desc())
                    .limit(1)
                    .mapNotNull { row -> Status.fromRow(table, row) }
                    .lastOrNull()
            } catch (e: PSQLException) {
                logger.error("Failed to query the database for service '$name'", e)
                null
            }
        }

        @Resource("history")
        class History(val parent: Service, val limit: Int = 1000, val from: Instant = JavaInstant.EPOCH.toKotlinInstant(), val to: Instant = Clock.System.now()) : ILoggable {
            suspend fun response(database: Database): List<Status> = withContext(Dispatchers.IO) {
                val table = StatusTable(parent.name)

                return@withContext try {
                    database
                        .from(table)
                        .select()
                        .where(table.timestamp.between(from.toJavaInstant()..to.toJavaInstant()))
                        .orderBy(table.timestamp.desc())
                        .limit(limit)
                        .mapNotNull { row -> Status.fromRow(table, row) }
                } catch (e: PSQLException) {
                    logger.error("Failed to query the database for history of service '${parent.name}'", e)
                    emptyList()
                }
            }
        }

        @Resource("uptime")
        class Uptime(val parent: Service, val from: Instant = JavaInstant.EPOCH.toKotlinInstant(), val to: Instant = Clock.System.now()) : ILoggable {
            @Serializable
            data class ServiceUptimeResponse(
                val onlineCount: Long = 0,
                val offlineCount: Long = 0,
                val uptime: Double = 0.0,
                val from: Instant = JavaInstant.EPOCH.toKotlinInstant(),
                val to: Instant = Clock.System.now(),
            ) {
                companion object {
                    fun fromCounts(onlineCount: Long, offlineCount: Long, from: Instant, to: Instant): ServiceUptimeResponse {
                        val totalCount = onlineCount + offlineCount
                        val uptimePercentage = if (totalCount > 0) onlineCount.toDouble() / totalCount else 0.0

                        return ServiceUptimeResponse(
                            onlineCount = onlineCount,
                            offlineCount = offlineCount,
                            uptime = uptimePercentage,
                            from = from,
                            to = to,
                        )
                    }
                }
            }

            suspend fun response(database: Database): ServiceUptimeResponse = withContext(Dispatchers.IO) {
                val table = StatusTable(parent.name)

                return@withContext try {
                    val status = database
                        .from(table)
                        .select(table.online)
                        .where(table.timestamp.between(from.toJavaInstant()..to.toJavaInstant()))
                        .mapNotNull { row -> row[table.online] }

                    ServiceUptimeResponse.fromCounts(
                        onlineCount = status.count { it }.toLong(),
                        offlineCount = status.count { !it }.toLong(),
                        from = from,
                        to = to,
                    )
                } catch (e: PSQLException) {
                    logger.error("Failed to query the database for uptime of service '${parent.name}'", e)
                    ServiceUptimeResponse()
                }
            }
        }
    }
}
