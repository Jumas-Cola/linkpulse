package com.linkpulse.monitor.adapter.output.persistence

import com.linkpulse.domain.model.MonitoredUrl
import com.linkpulse.domain.model.UrlId
import com.linkpulse.domain.model.UrlStatus
import com.linkpulse.domain.model.UserId
import com.linkpulse.monitor.adapter.output.persistence.tables.UsersTable
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.OffsetDateTime

class ExposedUrlRepositoryTest : FunSpec({
    TestDatabase

    val repo = ExposedUrlRepository()

    beforeEach {
        transaction {
            exec("TRUNCATE TABLE check_results, monitored_urls, users RESTART IDENTITY CASCADE")
        }
    }

    fun createUser(username: String = "user_${System.nanoTime()}"): Long = transaction {
        UsersTable.insert {
            it[UsersTable.username] = username
            it[UsersTable.password] = "hashed"
            it[UsersTable.createdAt] = OffsetDateTime.now()
        }[UsersTable.id].value
    }

    fun url(ownerId: Long, url: String = "https://example.com", status: UrlStatus = UrlStatus.UNKNOWN) =
        MonitoredUrl(
            url = url,
            name = "Test",
            intervalSeconds = 60,
            owner = UserId(ownerId),
            currentStatus = status,
        )

    test("findById returns null when url does not exist") {
        repo.findById(UrlId(999L)).shouldBeNull()
    }

    test("save inserts new url and assigns generated id") {
        val saved = repo.save(url(createUser()))
        saved.id.shouldNotBeNull()
        saved.id!!.value shouldNotBe 0L
    }

    test("findById returns saved url") {
        val ownerId = createUser()
        val saved = repo.save(url(ownerId, "https://example.com"))

        val found = repo.findById(saved.id!!)
        found.shouldNotBeNull()
        found.url shouldBe "https://example.com"
        found.owner shouldBe UserId(ownerId)
    }

    test("findByOwner returns only urls for that owner") {
        val owner1 = createUser("owner1")
        val owner2 = createUser("owner2")
        repo.save(url(owner1, "https://one.com"))
        repo.save(url(owner2, "https://two.com"))

        val result = repo.findByOwner(UserId(owner1))
        result.size shouldBe 1
        result.first().url shouldBe "https://one.com"
    }

    test("findAllActive returns all urls") {
        val ownerId = createUser()
        repo.save(url(ownerId, "https://a.com"))
        repo.save(url(ownerId, "https://b.com"))

        repo.findAllActive().size shouldBe 2
    }

    test("save updates currentStatus and consecutiveFailures") {
        val saved = repo.save(url(createUser()))
        val updated = saved.copy(currentStatus = UrlStatus.DOWN, consecutiveFailures = 3)
        repo.save(updated)

        val found = repo.findById(saved.id!!)!!
        found.currentStatus shouldBe UrlStatus.DOWN
        found.consecutiveFailures shouldBe 3
    }

    test("delete removes url") {
        val saved = repo.save(url(createUser()))
        repo.delete(saved.id!!)
        repo.findById(saved.id!!).shouldBeNull()
    }
})
