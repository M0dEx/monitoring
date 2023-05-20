package eu.m0dex.monitoring.monitor

import eu.m0dex.monitoring.statusapi.schema.Status
import io.questdb.client.Sender
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive

@OptIn(DelicateCoroutinesApi::class)
class QuestDBWriter(
    private val questDbSender: Sender,
    private val questDbChannel: Channel<Pair<String, Status>>
) {
    suspend fun run() = coroutineScope {
        while (isActive && !questDbChannel.isClosedForReceive) {
            val (name, status) = questDbChannel.receive()

            val row = questDbSender.table(name)

            row.boolColumn("online", status.online)

            status.responseCode?.let { row.longColumn("responseCode", it) }
            status.latency?.let { row.longColumn("latency", it) }
            status.failReason?.let { row.stringColumn("failReason", it) }

            status.timestamp?.let { row.at(it.epochSeconds) } ?: row.atNow()
        }
    }
}