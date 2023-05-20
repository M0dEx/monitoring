package eu.m0dex.monitoring.statusapi

import eu.m0dex.monitoring.ApiConfig
import eu.m0dex.monitoring.monitor.MonitoredService
import eu.m0dex.monitoring.service.ILoggable
import eu.m0dex.monitoring.statusapi.resources.Services
import eu.m0dex.monitoring.statusapi.schema.Status
import eu.m0dex.monitoring.statusapi.schema.StatusTable
import freemarker.cache.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.freemarker.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.toJavaInstant
import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.postgresql.util.PSQLException

@Suppress("ExtractKtorModule")
class StatusApi(
        config: ApiConfig,
        private val questDbReader: Database,
        private val monitoredServices: Set<MonitoredService>,
) : ILoggable {
    private val server = embeddedServer(
            Netty,
            host = config.bindAddress,
            port = config.bindPort,
    ) {
        install(Resources)
        install(FreeMarker) {
            templateLoader = ClassTemplateLoader(this::class.java.classLoader, "templates")
        }
        install(ContentNegotiation) {
            json()
        }
        routing {
            staticResources("/static", "static")
            get<Services> {
                call.respond(monitoredServices.map { service ->
                    mapOf(
                            "name" to service.name,
                            "displayName" to service.displayName,
                            "url" to service.url,
                    )
                })
            }
            get<Services.Get> { serviceQuery ->
                val table = StatusTable(serviceQuery.name)

                try {
                    questDbReader
                            .from(table)
                            .select()
                            .where(table.timestamp.lessEq(serviceQuery.closestBefore.toJavaInstant()))
                            .orderBy(table.timestamp.desc())
                            .limit(1)
                            .mapNotNull { row -> Status.fromRow(table, row) }
                            .lastOrNull()
                            ?.let { call.respond(it) }
                            ?: call.respond(HttpStatusCode.NotFound)
                } catch (e: PSQLException) {
                    logger.warn("Encountered PSQLException while querying QuestDB", e)
                    call.respond(HttpStatusCode.NotFound)
                }
            }
            get<Services.Get.History> { uptimeQuery ->
                val table = StatusTable(uptimeQuery.parent.name)

                try {
                    questDbReader
                            .from(table)
                            .select()
                            .where(table.timestamp.between(uptimeQuery.from.toJavaInstant()..uptimeQuery.to.toJavaInstant()))
                            .orderBy(table.timestamp.desc())
                            .mapNotNull { row -> Status.fromRow(table, row) }
                            .let { call.respond(it) }
                } catch (e: PSQLException) {
                    logger.warn("Encountered PSQLException while querying QuestDB", e)
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
    }

    suspend fun run() = withContext(Dispatchers.IO) {
        server.start(wait = true)
    }
}
