package eu.m0dex.monitoring

import eu.m0dex.monitoring.monitor.Monitor
import eu.m0dex.monitoring.monitor.MonitoredService
import eu.m0dex.monitoring.monitor.QuestDBWriter
import eu.m0dex.monitoring.monitor.StatusMessage
import eu.m0dex.monitoring.service.ILoggable
import eu.m0dex.monitoring.service.IService
import io.questdb.client.Sender
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.Database

class MonitoringService(
    override val config: MonitoringServiceConfig,
    private val questDbSender: Sender,
    private val questDbReader: Database,
) : IService, ILoggable {

    private val questDbChannel = Channel<StatusMessage>(Channel.UNLIMITED)

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

        joinAll(
            launch { questDbWriter.run() },
            launch { monitor.run() }
        )

        // TODO: Add a REST API provider and a web UI
    }
}