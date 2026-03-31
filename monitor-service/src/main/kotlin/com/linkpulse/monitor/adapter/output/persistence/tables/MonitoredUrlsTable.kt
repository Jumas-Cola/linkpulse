package com.linkpulse.monitor.adapter.output.persistence.tables

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone

object MonitoredUrlsTable : LongIdTable("monitored_urls") {
    val url                 = varchar("url", 2048)
    val name                = varchar("name", 256)
    val intervalSeconds     = integer("interval_seconds").default(60)
    val ownerId             = long("owner_id").references(UsersTable.id)
    val currentStatus       = varchar("current_status", 16).default("UNKNOWN")
    val consecutiveFailures = integer("consecutive_failures").default(0)
    val createdAt           = timestampWithTimeZone("created_at")
}
