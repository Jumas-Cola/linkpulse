package com.linkpulse.monitor.adapter.output

import at.favre.lib.crypto.bcrypt.BCrypt
import com.linkpulse.domain.model.HashedPassword
import com.linkpulse.domain.port.PasswordHasher

class BCryptPasswordHasher : PasswordHasher {
    override fun hash(raw: String): HashedPassword =
        HashedPassword(BCrypt.withDefaults().hashToString(12, raw.toCharArray()))

    override fun verify(raw: String, hash: HashedPassword): Boolean =
        BCrypt.verifyer().verify(raw.toCharArray(), hash.value.toCharArray()).verified
}