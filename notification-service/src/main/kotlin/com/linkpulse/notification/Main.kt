package com.linkpulse.notification

import io.github.oshai.kotlinlogging.KotlinLogging
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.coAwait

private val logger = KotlinLogging.logger {}

suspend fun main() {
    val vertx = Vertx.vertx()

    try {
        vertx.deployVerticle(EventListenerVerticle()).coAwait()
        logger.info { "✅ notification-service started" }
    } catch (e: Exception) {
        logger.error(e) { "❌ Failed to start notification-service" }
        vertx.close().coAwait()
    }
}
