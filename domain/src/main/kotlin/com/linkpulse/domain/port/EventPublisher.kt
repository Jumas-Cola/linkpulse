package com.linkpulse.domain.port

import com.linkpulse.domain.event.DomainEvent

interface EventPublisher {
    suspend fun publish(event: DomainEvent)
}
