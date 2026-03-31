package com.linkpulse.domain.port

import com.linkpulse.domain.model.CheckResult

interface HealthChecker {
    suspend fun check(url: String, timeoutMs: Long = 10_000): CheckResult
}
