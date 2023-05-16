package eu.m0dex.monitoring

import eu.m0dex.monitoring.monitor.Monitor
import eu.m0dex.monitoring.service.ILoggable
import eu.m0dex.monitoring.service.IService
import io.questdb.client.Sender
import kotlinx.coroutines.coroutineScope
import org.jetbrains.exposed.sql.Database

class MonitoringService(
    override val config: MonitoringServiceConfig,
    private val questDbSender: Sender,
    private val questDbReader: Database,
) : IService, ILoggable {
    override suspend fun run() = coroutineScope {
        Monitor(
            questDbSender,
            config.monitored,
        ).run()

        // TODO: Add a REST API provider and a web UI
    }
}