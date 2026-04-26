package com.linkpulse.domain.service

import com.linkpulse.domain.model.Email
import com.linkpulse.domain.model.User
import com.linkpulse.domain.model.UserId
import com.linkpulse.domain.port.PasswordHasher
import com.linkpulse.domain.port.auth.UserRegistrar
import com.linkpulse.domain.port.UserRepository
import com.linkpulse.domain.port.auth.UserLoginer

class UserService(
    private val userRepo: UserRepository,
    private val passwordHasher: PasswordHasher
) : UserRegistrar, UserLoginer {
    override suspend fun register(username: String, password: String): User {
        require(username.isNotBlank()) { "Username must not be blank" }
        require(password.length >= 8) { "Password must be at least 8 characters" }

        val existing = userRepo.findByEmail(Email(username))
        check(existing == null) { "Username already taken" }

        return userRepo.save(
            User(
                id = null,
                username = username,
                password = passwordHasher.hash(password)
            )
        )
    }

    override suspend fun login(username: String, password: String): User {
        require(username.isNotBlank()) { "Username must not be blank" }

        val user = userRepo.findByEmail(Email(username))
            ?: throw NoSuchElementException("Invalid credentials")
        check(passwordHasher.verify(password, user.password)) { "Invalid credentials" }

        return user
    }
}
