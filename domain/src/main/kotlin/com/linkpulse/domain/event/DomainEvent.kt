package com.linkpulse.domain.event

import com.linkpulse.domain.model.UrlId
import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
sealed interface DomainEvent {
    val urlId: Long
    val occurredAt: Instant
}

@Serializable
data class UrlWentDown(
    override val urlId: Long,
    val url: String,
    override val occurredAt: Instant
) : DomainEvent

@Serializable
data class UrlRecovered(
    override val urlId: Long,
    val url: String,
    override val occurredAt: Instant
) : DomainEvent

val DomainEvent.typedUrlId: UrlId get() = UrlId(urlId)
