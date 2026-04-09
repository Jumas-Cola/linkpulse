package com.linkpulse.domain.port

import com.linkpulse.domain.model.Email
import com.linkpulse.domain.model.MonitoredUrl
import com.linkpulse.domain.model.User
import com.linkpulse.domain.model.UserId

interface UserRepository {
    suspend fun findById(id: UserId): User?
    suspend fun findByEmail(email: Email): User?
    suspend fun save(user: User): User
    suspend fun delete(id: UserId)
}