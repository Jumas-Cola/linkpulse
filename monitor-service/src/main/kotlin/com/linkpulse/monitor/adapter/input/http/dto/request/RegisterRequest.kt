package com.linkpulse.monitor.adapter.input.http.dto.request

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val username: String,
    val password: String
)