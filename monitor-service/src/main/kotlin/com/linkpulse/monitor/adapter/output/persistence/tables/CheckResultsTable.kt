package com.linkpulse.monitor.adapter.output.persistence.tables

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone

object CheckResultsTable : LongIdTable("check_results") {
    val urlId     = long("url_id").references(MonitoredUrlsTable.id)
    val httpStatus = integer("http_status").nullable()
    val latencyMs = long("latency_ms")
    val error     = text("error").nullable()
    val checkedAt = timestampWithTimeZone("checked_at")
}
