package com.linkpulse.monitor.adapter.output.persistence

import com.linkpulse.domain.model.CheckResult
import com.linkpulse.domain.model.MonitoredUrl
import com.linkpulse.domain.model.UrlId
import com.linkpulse.domain.model.UserId
import com.linkpulse.monitor.adapter.output.persistence.tables.UsersTable
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.OffsetDateTime

class ExposedCheckResultRepositoryTest : FunSpec({
    TestDatabase

    val repo = ExposedCheckResultRepository()
    val urlRepo = ExposedUrlRepository()

    beforeEach {
        transaction {
            exec("TRUNCATE TABLE check_results, monitored_urls, users RESTART IDENTITY CASCADE")
        }
    }

    fun createUser(): Long = transaction {
        UsersTable.insert {
            it[username] = "user_${System.nanoTime()}"
            it[password] = "hashed"
            it[createdAt] = OffsetDateTime.now()
        }[UsersTable.id].value
    }

    suspend fun createUrl(ownerId: Long): MonitoredUrl =
        urlRepo.save(
            MonitoredUrl(
                id = UrlId(0),
                url = "https://example.com",
                name = "Test",
                intervalSeconds = 60,
                owner = UserId(ownerId),
            )
        )

    fun result(httpStatus: Int = 200, latencyMs: Long = 100L, checkedAt: kotlin.time.Instant = Clock.System.now()) =
        CheckResult(httpStatus = httpStatus, latencyMs = latencyMs, error = null, checkedAt = checkedAt)

    test("findByUrlId returns empty list when no results exist") {
        repo.findByUrlId(UrlId(999L)).shouldBeEmpty()
    }

    test("save persists check result") {
        val url = createUrl(createUser())
        repo.save(url.id, result(httpStatus = 200, latencyMs = 150))

        val results = repo.findByUrlId(url.id)
        results.shouldHaveSize(1)
        results.first().httpStatus shouldBe 200
        results.first().latencyMs shouldBe 150
    }

    test("save persists error result") {
        val url = createUrl(createUser())
        repo.save(url.id, CheckResult(httpStatus = null, latencyMs = 0, error = "timeout"))

        val results = repo.findByUrlId(url.id)
        results.shouldHaveSize(1)
        results.first().error shouldBe "timeout"
    }

    test("findByUrlId returns results ordered by checkedAt DESC") {
        val url = createUrl(createUser())
        val now = Clock.System.now()
        repo.save(url.id, result(checkedAt = now - 2.hours))
        repo.save(url.id, result(checkedAt = now - 1.hours))
        repo.save(url.id, result(checkedAt = now))

        val results = repo.findByUrlId(url.id)
        results.shouldHaveSize(3)
        (results[0].checkedAt > results[1].checkedAt) shouldBe true
        (results[1].checkedAt > results[2].checkedAt) shouldBe true
    }

    test("findByUrlId respects limit") {
        val url = createUrl(createUser())
        repeat(5) { repo.save(url.id, result()) }

        repo.findByUrlId(url.id, limit = 3).shouldHaveSize(3)
    }

    test("findByUrlId respects offset") {
        val url = createUrl(createUser())
        repeat(5) { repo.save(url.id, result()) }

        repo.findByUrlId(url.id, limit = 10, offset = 3).shouldHaveSize(2)
    }
})
