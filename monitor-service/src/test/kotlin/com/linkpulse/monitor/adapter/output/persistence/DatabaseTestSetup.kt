package com.linkpulse.monitor.adapter.output.persistence

import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import org.testcontainers.containers.PostgreSQLContainer

internal object TestDatabase {
    init {
        // docker-java 3.4.0 defaults to API 1.32; Docker 29.x requires min 1.40
        System.setProperty("api.version", "1.41")

        val container = PostgreSQLContainer<Nothing>("postgres:16-alpine").also { it.start() }

        Database.connect(
            url = container.jdbcUrl,
            driver = "org.postgresql.Driver",
            user = container.username,
            password = container.password,
        )
        Flyway.configure()
            .dataSource(container.jdbcUrl, container.username, container.password)
            .locations("classpath:db/migration")
            .load()
            .migrate()
    }
}
