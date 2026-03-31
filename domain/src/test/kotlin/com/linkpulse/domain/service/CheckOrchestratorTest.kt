package com.linkpulse.domain.service

import com.linkpulse.domain.model.*
import com.linkpulse.domain.event.UrlWentDown
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.time.Clock

class CheckOrchestratorTest : FunSpec({

    test("applyCheck should detect transition from UP to DOWN") {
        val url = MonitoredUrl(
            id = UrlId(1),
            url = "https://example.com",
            name = "Example",
            intervalSeconds = 60,
            owner = UserId(1),
            currentStatus = UrlStatus.UP
        )

        val failedResult = CheckResult(
            httpStatus = null,
            latencyMs = 0,
            error = "Connection refused",
            checkedAt = Clock.System.now()
        )

        val (updated, event) = url.applyCheck(failedResult)

        updated.currentStatus shouldBe UrlStatus.DOWN
        updated.consecutiveFailures shouldBe 1
        event.shouldBeInstanceOf<UrlWentDown>()
    }

    test("applyCheck should return no event when status stays the same") {
        val url = MonitoredUrl(
            id = UrlId(1),
            url = "https://example.com",
            name = "Example",
            intervalSeconds = 60,
            owner = UserId(1),
            currentStatus = UrlStatus.UP
        )

        val okResult = CheckResult(
            httpStatus = 200,
            latencyMs = 150,
            error = null,
            checkedAt = Clock.System.now()
        )

        val (updated, event) = url.applyCheck(okResult)

        updated.currentStatus shouldBe UrlStatus.UP
        updated.consecutiveFailures shouldBe 0
        event shouldBe null
    }

    test("DEGRADED status for slow responses") {
        val result = CheckResult(httpStatus = 200, latencyMs = 6000, error = null)
        result.deriveStatus() shouldBe UrlStatus.DEGRADED
    }
})
