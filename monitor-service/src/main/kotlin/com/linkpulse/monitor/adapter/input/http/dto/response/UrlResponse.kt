package com.linkpulse.monitor.adapter.input.http.dto.response

import com.linkpulse.domain.model.MonitoredUrl
import kotlinx.serialization.Serializable

@Serializable
data class UrlResponse(
    val id: Long,
    val url: String,
    val name: String,
    val intervalSeconds: Int,
    val status: String,
    val consecutiveFailures: Int,
    val createdAt: String
) {
    companion object {
        fun from(domain: MonitoredUrl) = UrlResponse(
            id = domain.id!!.value,
            url = domain.url,
            name = domain.name,
            intervalSeconds = domain.intervalSeconds,
            status = domain.currentStatus.name,
            consecutiveFailures = domain.consecutiveFailures,
            createdAt = domain.createdAt.toString()
        )
    }
}
