package com.linkpulse.domain.model

import kotlin.time.Clock
import kotlin.time.Instant

data class CheckResult(
    val httpStatus: Int?,
    val latencyMs: Long,
    val error: String?,
    val checkedAt: Instant = Clock.System.now()
) {
    fun deriveStatus(): UrlStatus = when {
        error != null            -> UrlStatus.DOWN
        httpStatus in 200..399   -> if (latencyMs > 5000) UrlStatus.DEGRADED else UrlStatus.UP
        else                     -> UrlStatus.DOWN
    }
}
