package com.linkpulse.monitor.adapter.input.http.dto.request

import com.linkpulse.domain.model.UrlId
import kotlinx.serialization.Serializable

@Serializable
data class HistoryRequest(
    val urlId: String,
    val limit: Int = 50,
    val offset: Int = 0,
)
