package com.linkpulse.monitor

import com.linkpulse.monitor.di.appModule
import com.linkpulse.monitor.scheduler.CheckSchedulerVerticle
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.oshai.kotlinlogging.KotlinLogging
import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.PubSecKeyOptions
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.auth.jwt.JWTAuthOptions
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions
import io.vertx.kotlin.coroutines.coAwait
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module

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

        val poolSize = config.getJsonObject("database")?.getJsonObject("pool")?.getInteger("maxSize") ?: 10
        val hikari = HikariDataSource(HikariConfig().apply {
            jdbcUrl = dbUrl
            username = dbUser
            password = dbPassword
            maximumPoolSize = poolSize
            driverClassName = "org.postgresql.Driver"
        })
        Database.connect(hikari)

        val jwtSecret = config.getJsonObject("jwt")?.getString("secret") ?: "dev-secret"
        val jwtAuth = JWTAuth.create(vertx, JWTAuthOptions()
            .addPubSecKey(PubSecKeyOptions()
                .setAlgorithm("HS256")
                .setBuffer(jwtSecret)
            )
        )

        val webClient = WebClient.create(vertx, WebClientOptions()
            .setVerifyHost(false)
            .setDefaultPort(443)
            .setDefaultHost("")
        )

        val checkerTimeout = config.getJsonObject("checker")?.getLong("timeoutMs") ?: 10_000L
        val maxConcurrency = config.getJsonObject("checker")?.getInteger("maxConcurrency") ?: 20
        val checkInterval = config.getJsonObject("checker")?.getLong("intervalMs") ?: 60_000L

        startKoin {
            modules(
                appModule,
                module {
                    single { vertx }
                    single { jwtAuth }
                    single { webClient }
                    single(named("maxConcurrency")) { maxConcurrency }
                    single(named("checker.intervalMs")) { checkInterval }
                    single { checkerTimeout }
                }
            )
        }

        vertx.deployVerticle(CheckSchedulerVerticle())
        vertx.deployVerticle(ApiVerticle()).coAwait()
        logger.info { "✅ monitor-service started on port 8080" }
    } catch (e: Exception) {
        logger.error(e) { "❌ Failed to start monitor-service" }
        vertx.close().coAwait()
    }
}
