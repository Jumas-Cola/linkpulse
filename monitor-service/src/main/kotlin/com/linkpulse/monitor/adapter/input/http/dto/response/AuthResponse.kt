package com.linkpulse.monitor.adapter.input.http.dto.response

import kotlinx.serialization.Serializable

@Serializable
data class AuthResponse(val token: String)
