package eu.m0dex.monitoring.statusapi.schema

import kotlinx.datetime.Instant
import kotlinx.datetime.toKotlinInstant
import kotlinx.serialization.Serializable
import org.ktorm.dsl.QueryRowSet
import java.time.ZoneId

@Serializable
data class Status(
    val online: Boolean,
    val responseCode: Long? = null,
    val latency: Long? = null,
    val failReason: String? = null,
    val timestamp: Instant? = null,
) {
    companion object {
        fun fromRow(table: StatusTable, row: QueryRowSet): Status? {
            val online = row[table.online] ?: return null

            return Status(
                online = online,
                responseCode = row[table.responseCode],
                latency = row[table.latency],
                failReason = row[table.failReason],
                // TODO: Maybe there is a better way, but I have not found it after 3 hours of looking
                timestamp = row[table.timestamp]
                        ?.atZone(ZoneId.systemDefault())
                        ?.withZoneSameLocal(ZoneId.of("UTC"))
                        ?.toInstant()
                        ?.toKotlinInstant(),
            )
        }
    }
}
