package com.linkpulse.monitor.adapter.input.http.dto.request

import kotlinx.serialization.Serializable

@Serializable
data class UrlRequest(
    val url: String,
    val name: String? = null,
    val intervalSeconds: Int
)
