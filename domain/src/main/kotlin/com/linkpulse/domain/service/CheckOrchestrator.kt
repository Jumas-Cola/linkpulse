package com.linkpulse.domain.service

import com.linkpulse.domain.model.CheckResult
import com.linkpulse.domain.model.MonitoredUrl
import com.linkpulse.domain.port.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.time.Clock

class CheckOrchestrator(
    private val urlRepo: UrlRepository,
    private val checker: HealthChecker,
    private val resultRepo: CheckResultRepository,
    private val publisher: EventPublisher,
    private val maxConcurrency: Int = 20
) {
    suspend fun runAllChecks() = coroutineScope {
        val semaphore = Semaphore(maxConcurrency)

        urlRepo.findAllActive().map { url ->
            async {
                semaphore.withPermit {
                    checkSingle(url)
                }
            }
        }.awaitAll()
    }

    private suspend fun checkSingle(url: MonitoredUrl) {
        val result = try {
            checker.check(url.url)
        } catch (e: Exception) {
            CheckResult(
                httpStatus = null,
                latencyMs = 0,
                error = e.message ?: "Unknown error",
                checkedAt = Clock.System.now()
            )
        }

        resultRepo.save(url.id!!, result)

        val (updated, event) = url.applyCheck(result)
        urlRepo.save(updated)
        event?.let { publisher.publish(it) }
    }
}
