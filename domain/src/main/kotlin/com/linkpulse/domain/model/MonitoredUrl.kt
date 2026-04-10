package com.linkpulse.domain.model

import com.linkpulse.domain.event.DomainEvent
import com.linkpulse.domain.event.UrlRecovered
import com.linkpulse.domain.event.UrlWentDown
import kotlin.time.Clock
import kotlin.time.Instant

data class MonitoredUrl(
    val id: UrlId? = null,
    val url: String,
    val name: String,
    val intervalSeconds: Int,
    val owner: UserId,
    val currentStatus: UrlStatus = UrlStatus.UNKNOWN,
    val consecutiveFailures: Int = 0,
    val createdAt: Instant = Clock.System.now()
) {
    fun applyCheck(result: CheckResult): Pair<MonitoredUrl, DomainEvent?> {
        val newStatus = result.deriveStatus()
        val event = detectTransition(currentStatus, newStatus)
        return copy(
            currentStatus = newStatus,
            consecutiveFailures = if (newStatus == UrlStatus.UP) 0 else consecutiveFailures + 1
        ) to event
    }

    private fun detectTransition(old: UrlStatus, new: UrlStatus): DomainEvent? = when {
        old == UrlStatus.UP && new == UrlStatus.DOWN   -> UrlWentDown(id!!.value, url, Clock.System.now())
        old == UrlStatus.DOWN && new == UrlStatus.UP   -> UrlRecovered(id!!.value, url, Clock.System.now())
        else -> null
    }
}
