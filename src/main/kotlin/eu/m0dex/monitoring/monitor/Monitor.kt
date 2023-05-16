package eu.m0dex.monitoring.monitor

import eu.m0dex.monitoring.MonitoredServiceConfig
import eu.m0dex.monitoring.service.ILoggable
import io.questdb.client.Sender
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

class Monitor(
    private val questDbSender: Sender,
    serviceConfigs: Map<String, MonitoredServiceConfig>,
) : ILoggable {

    private val questDbChannel = Channel<StatusMessage>()

    private val monitoredServices: Set<MonitoredService> =
        serviceConfigs.map { (serviceName, serviceConfig) ->
            MonitoredService(
                questDbChannel = questDbChannel,
                name = serviceName,
                displayName = serviceConfig.displayName,
                url = serviceConfig.url,
                method = serviceConfig.method,
                expectedResponseRegex = serviceConfig.expectedResponseRegex,
                additionalHeaders = serviceConfig.additionalHeaders,
                intervalMillis = serviceConfig.intervalMillis,
            )
        }.toSet()

    suspend fun run() = coroutineScope {
        val questDbWriter = QuestDBWriter(questDbSender, questDbChannel)

        val jobs = monitoredServices
            .map { launch { it.checkAvailability() }}
            .toMutableList()

        jobs.add(launch { questDbWriter.run() })

        jobs.joinAll()
    }
}