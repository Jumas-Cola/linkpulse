package com.linkpulse.monitor.adapter.output.persistence

import com.linkpulse.domain.model.Email
import com.linkpulse.domain.model.HashedPassword
import com.linkpulse.domain.model.User
import com.linkpulse.domain.model.UserId
import com.linkpulse.domain.port.UserRepository
import com.linkpulse.monitor.adapter.output.persistence.tables.UsersTable
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

class ExposedUserRepository : AbstractRepository(), UserRepository {
    override suspend fun findById(id: UserId): User? =
        dbQuery {
            UsersTable.selectAll()
                .where { UsersTable.id eq id.value }
                .singleOrNull()
                ?.toDomain()
        }

    override suspend fun findByEmail(email: Email): User? =
        dbQuery {
            UsersTable.selectAll()
                .where { UsersTable.username eq email.value }
                .singleOrNull()
                ?.toDomain()
        }

    override suspend fun save(user: User): User =
        dbQuery {
            if (user.id == null) {
                val newId = UsersTable.insert {
                    it[this.username] = user.username
                    it[this.password] = user.password.value
                }[UsersTable.id]
                user.copy(id = UserId(newId.value))
            } else {
                UsersTable.update({ UsersTable.id eq user.id!!.value }) {
                    it[UsersTable.username] = user.username
                    it[UsersTable.password] = user.password.value
                }
                user
            }
        }

    override suspend fun delete(id: UserId): Unit =
        dbQuery {
            UsersTable.deleteWhere { UsersTable.id eq id.value }
        }

    private fun ResultRow.toDomain(): User =
        User(
            id = UserId(this[UsersTable.id].value),
            username = this[UsersTable.username],
            password = HashedPassword(this[UsersTable.password]),
        )
}