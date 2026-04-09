package com.linkpulse.domain.port

import com.linkpulse.domain.model.HashedPassword

interface PasswordHasher {
    fun hash(raw: String): HashedPassword
    fun verify(raw: String, hash: HashedPassword): Boolean
}