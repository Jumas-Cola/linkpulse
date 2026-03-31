package com.linkpulse.notification

import com.linkpulse.domain.event.DomainEvent
import com.linkpulse.domain.event.UrlRecovered
import com.linkpulse.domain.event.UrlWentDown
import io.github.oshai.kotlinlogging.KotlinLogging
import io.vertx.kotlin.coroutines.CoroutineVerticle
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

private val logger = KotlinLogging.logger {}
private val json = Json { ignoreUnknownKeys = true }

class EventListenerVerticle : CoroutineVerticle() {

    override suspend fun start() {
        vertx.eventBus().consumer<String>("events.url.down") { message ->
            launch {
                val event = json.decodeFromString<UrlWentDown>(message.body())
                // TODO: заменить на NotificationSender
                logger.warn { "🔴 DOWN: ${event.url} (id=${event.urlId})" }
            }
        }

        vertx.eventBus().consumer<String>("events.url.recovered") { message ->
            launch {
                val event = json.decodeFromString<UrlRecovered>(message.body())
                logger.info { "🟢 RECOVERED: ${event.url} (id=${event.urlId})" }
            }
        }

        logger.info { "Event listener subscribed to events.url.down, events.url.recovered" }
    }
}
