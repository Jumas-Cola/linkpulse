package com.linkpulse.monitor.adapter.output.persistence

import com.linkpulse.domain.model.CheckResult
import com.linkpulse.domain.model.UrlId
import com.linkpulse.domain.port.CheckResultRepository
import com.linkpulse.monitor.adapter.output.persistence.tables.CheckResultsTable
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.ZoneOffset

class ExposedCheckResultRepository : AbstractRepository(), CheckResultRepository {
    override suspend fun save(urlId: UrlId, result: CheckResult): Unit =
        dbQuery {
            CheckResultsTable.insert {
                it[this.urlId] = urlId.value
                it[httpStatus] = result.httpStatus
                it[latencyMs] = result.latencyMs
                it[error] = result.error
                it[checkedAt] = result.checkedAt.toJavaInstant().atOffset(ZoneOffset.UTC)
            }
        }

    override suspend fun findByUrlId(urlId: UrlId, limit: Int, offset: Int): List<CheckResult> =
        dbQuery {
            CheckResultsTable
                .selectAll()
                .where { CheckResultsTable.urlId eq urlId.value }
                .orderBy(CheckResultsTable.checkedAt, SortOrder.DESC)
                .limit(limit)
                .offset(offset.toLong())
                .map { it.toDomain() }
        }

    private fun ResultRow.toDomain(): CheckResult =
        CheckResult(
            httpStatus = this[CheckResultsTable.httpStatus],
            latencyMs = this[CheckResultsTable.latencyMs],
            error = this[CheckResultsTable.error],
            checkedAt = this[CheckResultsTable.checkedAt].toInstant().toKotlinInstant(),
        )
}
