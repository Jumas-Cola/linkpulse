package com.linkpulse.monitor.adapter.output.persistence.tables

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone

object UsersTable : LongIdTable("users") {
    val username  = varchar("username", 128).uniqueIndex()
    val password  = varchar("password", 256)
    val createdAt = timestampWithTimeZone("created_at")
}
