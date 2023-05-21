package eu.m0dex.monitoring.database.schema

import org.ktorm.schema.*

class StatusTable(tableName: String) : Table<Nothing>(tableName) {
    val online = boolean("online")
    val responseCode = long("responseCode")
    val latency = long("latency")
    val failReason = varchar("failReason")
    val timestamp = timestamp("timestamp")
}