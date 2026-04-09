package com.linkpulse.domain.port.auth

import com.linkpulse.domain.model.User

interface UserLoginer {
    suspend fun login(username: String, password: String): User
}