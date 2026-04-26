package com.linkpulse.monitor.adapter.output.eventbus

import com.linkpulse.domain.event.DomainEvent
import com.linkpulse.domain.event.UrlRecovered
import com.linkpulse.domain.event.UrlWentDown
import com.linkpulse.domain.port.EventPublisher
import io.vertx.core.Vertx
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class VertxEventPublisher(
    private val vertx: Vertx
) : EventPublisher {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun publish(event: DomainEvent) {
        val address = when (event) {
            is UrlWentDown -> "events.url.down"
            is UrlRecovered -> "events.url.recovered"
        }
        val message = json.encodeToString(event)
        vertx.eventBus().send(address, message)
    }
}