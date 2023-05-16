package eu.m0dex.monitoring.monitor

import io.questdb.client.Sender
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive

@OptIn(DelicateCoroutinesApi::class)
class QuestDBWriter(
    private val questDbSender: Sender,
    private val questDbChannel: Channel<StatusMessage>
) {
    suspend fun run() = coroutineScope {
        while (isActive && !questDbChannel.isClosedForReceive) {
            val statusMessage = questDbChannel.receive()

            statusMessage.writeToQuestDb(questDbSender)
        }
    }
}