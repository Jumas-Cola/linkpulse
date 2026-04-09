package com.linkpulse.monitor.adapter.input.http.dto.response

import com.linkpulse.domain.model.CheckResult
import kotlinx.serialization.Serializable

@Serializable
data class CheckResultResponse(
    val httpStatus: Int?,
    val latencyMs: Long,
    val status: String,
    val error: String?,
    val checkedAt: String
) {
    companion object {
        fun from(domain: CheckResult) = CheckResultResponse(
            httpStatus = domain.httpStatus,
            latencyMs = domain.latencyMs,
            status = domain.deriveStatus().name,
            error = domain.error,
            checkedAt = domain.checkedAt.toString()
        )
    }
}
