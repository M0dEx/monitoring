package eu.m0dex.monitoring.monitor

import eu.m0dex.monitoring.service.ILoggable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

class Monitor(
    private val monitoredServices: Set<MonitoredService>
) : ILoggable {

    suspend fun run() = coroutineScope {
        logger.info("Starting monitoring of ${monitoredServices.size} services")

        monitoredServices
            .map { launch { it.checkAvailability() } }
            .joinAll()
    }
}
