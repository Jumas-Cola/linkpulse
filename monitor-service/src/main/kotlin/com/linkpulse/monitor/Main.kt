package com.linkpulse.monitor

import io.github.oshai.kotlinlogging.KotlinLogging
import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.coAwait
import org.flywaydb.core.Flyway

private val logger = KotlinLogging.logger {}

fun runMigrations(jdbcUrl: String?, username: String?, password: String?) {
    val flyway = Flyway.configure()
        .dataSource(jdbcUrl, username, password)
        .load()

    flyway.migrate()
}

suspend fun main() {
    val vertx = Vertx.vertx(VertxOptions().apply {
        preferNativeTransport = true
    })

    val storeOptions = ConfigStoreOptions()
        .setType("file")
        .setFormat("hocon") // Specify HOCON format for .conf files
        .setConfig(JsonObject().put("path", "application.conf"))

    val retrieverOptions = ConfigRetrieverOptions().addStore(storeOptions)

    val retriever = ConfigRetriever.create(vertx, retrieverOptions)

    try {
        val config = retriever.getConfig().coAwait()
        val dbUrl = config.getJsonObject("database")?.getString("url")
        val dbUser = config.getJsonObject("database")?.getString("user")
        val dbPassword = config.getJsonObject("database")?.getString("password")

        runMigrations(dbUrl, dbUser, dbPassword)
        // TODO: задеплоить ApiVerticle, CheckSchedulerVerticle
        vertx.deployVerticle(ApiVerticle()).coAwait()
        logger.info { "✅ monitor-service started on port 8080" }
    } catch (e: Exception) {
        logger.error(e) { "❌ Failed to start monitor-service" }
        vertx.close().coAwait()
    }
}
