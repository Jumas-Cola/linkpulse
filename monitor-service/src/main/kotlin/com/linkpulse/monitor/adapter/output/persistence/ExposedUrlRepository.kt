package com.linkpulse.monitor.adapter.output.persistence

import com.linkpulse.domain.model.MonitoredUrl
import com.linkpulse.domain.model.UrlId
import com.linkpulse.domain.model.UrlStatus
import com.linkpulse.domain.model.UserId
import com.linkpulse.domain.port.UrlRepository
import com.linkpulse.monitor.adapter.output.persistence.tables.MonitoredUrlsTable
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

class ExposedUrlRepository : AbstractRepository(), UrlRepository {
    override suspend fun findById(id: UrlId): MonitoredUrl? =
        dbQuery {
            MonitoredUrlsTable
                .selectAll()
                .where { MonitoredUrlsTable.id eq id.value }
                .singleOrNull()
                ?.toDomain()
        }

    override suspend fun findByOwner(owner: UserId): List<MonitoredUrl> =
        dbQuery {
            MonitoredUrlsTable
                .selectAll()
                .where { MonitoredUrlsTable.ownerId eq owner.value }
                .map { it.toDomain() }
        }

    override suspend fun findAllActive(): List<MonitoredUrl> =
        dbQuery {
            MonitoredUrlsTable
                .selectAll()
                .map { it.toDomain() }
        }

    override suspend fun save(url: MonitoredUrl): MonitoredUrl =
        dbQuery {
            if (url.id == null) {
                val newId = MonitoredUrlsTable.insert {
                    it[this.url] = url.url
                    it[name] = url.name
                    it[intervalSeconds] = url.intervalSeconds
                    it[ownerId] = url.owner.value
                    it[currentStatus] = url.currentStatus.name
                    it[consecutiveFailures] = url.consecutiveFailures
                }[MonitoredUrlsTable.id]
                url.copy(id = UrlId(newId.value))
            } else {
                MonitoredUrlsTable.update({ MonitoredUrlsTable.id eq url.id!!.value }) {
                    it[currentStatus] = url.currentStatus.name
                    it[consecutiveFailures] = url.consecutiveFailures
                }
                url
            }
        }

    override suspend fun delete(id: UrlId): Unit =
        dbQuery {
            MonitoredUrlsTable.deleteWhere { MonitoredUrlsTable.id eq id.value }
        }

    private fun ResultRow.toDomain(): MonitoredUrl =
        MonitoredUrl(
            id = UrlId(this[MonitoredUrlsTable.id].value),
            url = this[MonitoredUrlsTable.url],
            name = this[MonitoredUrlsTable.name],
            intervalSeconds = this[MonitoredUrlsTable.intervalSeconds],
            owner = UserId(this[MonitoredUrlsTable.ownerId]),
            currentStatus = UrlStatus.valueOf(this[MonitoredUrlsTable.currentStatus]),
            consecutiveFailures = this[MonitoredUrlsTable.consecutiveFailures],
        )
}