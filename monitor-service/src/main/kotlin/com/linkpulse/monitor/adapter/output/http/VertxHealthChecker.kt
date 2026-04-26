package com.linkpulse.monitor.adapter.output.http

import com.linkpulse.domain.model.CheckResult
import com.linkpulse.domain.port.HealthChecker
import io.vertx.core.http.HttpMethod
import io.vertx.ext.web.client.WebClient
import io.vertx.kotlin.coroutines.coAwait
import kotlin.time.Clock

class VertxHealthChecker(
    private val webClient: WebClient,
    private val defaultTimeoutMs: Long = 10_000
) : HealthChecker {

    override suspend fun check(url: String, timeoutMs: Long): CheckResult {
        val start = Clock.System.now()
        return try {
            val response = webClient.requestAbs(HttpMethod.GET, url)
                .timeout(timeoutMs)
                .send()
                .coAwait()

            val latencyMs = Clock.System.now().minus(start).inWholeMilliseconds

            CheckResult(
                httpStatus = response.statusCode(),
                latencyMs = latencyMs,
                error = null,
                checkedAt = Clock.System.now()
            )
        } catch (e: Exception) {
            CheckResult(
                httpStatus = null,
                latencyMs = Clock.System.now().minus(start).inWholeMilliseconds,
                error = e.message,
                checkedAt = Clock.System.now()
            )
        }
    }
}