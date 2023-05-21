package eu.m0dex.monitoring

import eu.m0dex.monitoring.monitor.Monitor
import eu.m0dex.monitoring.monitor.MonitoredService
import eu.m0dex.monitoring.monitor.QuestDBWriter
import eu.m0dex.monitoring.database.schema.Status
import eu.m0dex.monitoring.service.ILoggable
import eu.m0dex.monitoring.service.IService
import eu.m0dex.monitoring.backend.BackendServer
import io.questdb.client.Sender
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import org.ktorm.database.Database

class MonitoringService(
    override val config: MonitoringServiceConfig,
    private val questDbSender: Sender,
    private val questDbReader: Database,
) : IService, ILoggable {

    private val questDbChannel = Channel<Pair<String, Status>>(Channel.UNLIMITED)

    private val monitoredServices = config.monitored.map { (serviceName, serviceConfig) ->
        MonitoredService(
            questDbChannel = questDbChannel,
            name = serviceName,
            displayName = serviceConfig.displayName,
            url = serviceConfig.url,
            method = serviceConfig.method,
            payload = serviceConfig.payload,
            additionalHeaders = serviceConfig.additionalHeaders,
            expectedStatusCodes = serviceConfig.expectedStatusCodes,
            expectedResponseRegex = serviceConfig.expectedResponseRegex,
            intervalMillis = serviceConfig.intervalMillis,
            timeoutMillis = serviceConfig.timeoutMillis,
        )
    }.toSet()

    override suspend fun run() = coroutineScope {
        val questDbWriter = QuestDBWriter(questDbSender, questDbChannel)
        val monitor = Monitor(monitoredServices)
        val backendServer = BackendServer(config.api, questDbReader, monitoredServices)

        joinAll(
            launch { questDbWriter.run() },
            launch { monitor.run() },
            launch { backendServer.run() },
        )
    }
}