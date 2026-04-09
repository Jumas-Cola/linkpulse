package com.linkpulse.monitor.adapter.input.http.dto.response

import kotlinx.serialization.Serializable

@Serializable
data class RegisterResponse(
    val userId: String,
    val username: String
)
