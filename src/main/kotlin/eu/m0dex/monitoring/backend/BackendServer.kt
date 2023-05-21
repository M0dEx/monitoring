package eu.m0dex.monitoring.backend

import eu.m0dex.monitoring.ApiConfig
import eu.m0dex.monitoring.backend.gui.resources.ServicesGui
import eu.m0dex.monitoring.backend.rest.resources.Services
import eu.m0dex.monitoring.monitor.MonitoredService
import eu.m0dex.monitoring.service.ILoggable
import freemarker.cache.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.freemarker.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.ktorm.database.Database

@Suppress("ExtractKtorModule")
class BackendServer(
    config: ApiConfig,
    private val database: Database,
    private val monitoredServices: Set<MonitoredService>,
) : ILoggable {
    private val server = embeddedServer(
        Netty,
        host = config.bindAddress,
        port = config.bindPort,
    ) {
        install(Compression) {
            gzip()
        }
        install(Resources)
        install(FreeMarker) {
            templateLoader = ClassTemplateLoader(this::class.java.classLoader, "templates")
        }
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                encodeDefaults = true
            })
        }

        routing {
            staticResources("/static", "static")
            get("/") {
                call.respondRedirect("/services")
            }
            get("/services") {
                call.respond(
                    FreeMarkerContent(
                        "services.ftl", mapOf(
                            "services" to ServicesGui().response(database, monitoredServices)
                        )
                    )
                )
            }
            get<Services> { services ->
                call.respond(services.response(monitoredServices))
            }
            get<Services.Service> { service ->
                call.respondNullable(service.response(database) ?: HttpStatusCode.NotFound)
            }
            get<Services.Service.History> { history ->
                call.respond(history.response(database))
            }
            get<Services.Service.Uptime> { uptime ->
                call.respond(uptime.response(database))
            }
        }
    }

    suspend fun run() = withContext(Dispatchers.IO) {
        server.start(wait = true)
    }
}
