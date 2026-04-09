package com.linkpulse.domain.port.auth

import com.linkpulse.domain.model.User

interface UserRegistrar {
    suspend fun register(username: String, password: String): User
}