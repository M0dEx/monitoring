package eu.m0dex.monitoring.monitor

import io.questdb.client.Sender

data class StatusMessage(
    val serviceName: String,
    val online: Boolean,
    val responseCode: Int,
    val latency: Long,
    val failReason: String? = null,
) {
    fun writeToQuestDb(questDbSender: Sender) {
        val row = questDbSender.table(serviceName)

        row.boolColumn("online", online)
            .longColumn("responseCode", responseCode.toLong())
            .longColumn("latency", latency)

        failReason?.let { row.stringColumn("failReason", it) }

        row.atNow()
    }
}
