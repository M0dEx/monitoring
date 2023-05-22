package eu.m0dex.monitoring.monitor

import eu.m0dex.monitoring.database.schema.Status
import io.questdb.client.Sender
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.datetime.Clock

@OptIn(DelicateCoroutinesApi::class)
class QuestDBWriter(
    private val questDbSender: Sender,
    private val questDbChannel: Channel<Pair<String, Status>>
) {
    suspend fun run() = withContext(Dispatchers.IO) {
        while (isActive && !questDbChannel.isClosedForReceive) {
            val (name, status) = questDbChannel.receive()

            val row = questDbSender.table(name)

            row.boolColumn("online", status.online)

            status.responseCode?.let { row.longColumn("responseCode", it) }
            status.latency?.let { row.longColumn("latency", it) }
            status.failReason?.let { row.stringColumn("failReason", it) }

            status.timestamp?.let { row.at(it.epochSeconds) } ?: row.at(Clock.System.now().toEpochMilliseconds())
        }
    }
}
