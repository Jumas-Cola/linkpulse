package com.linkpulse.domain.model

import kotlin.time.Clock
import kotlin.time.Instant

data class User(
    val id: UserId,
    val username: String,
    val password: HashedPassword,
    val createdAt: Instant = Clock.System.now()
)
