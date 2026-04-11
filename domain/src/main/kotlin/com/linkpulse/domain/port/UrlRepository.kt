package com.linkpulse.domain.port

import com.linkpulse.domain.model.MonitoredUrl
import com.linkpulse.domain.model.UrlId
import com.linkpulse.domain.model.UserId

interface UrlRepository {
    suspend fun findById(id: UrlId): MonitoredUrl?
    suspend fun findByIdAndOwner(id: UrlId, ownerId: UserId): MonitoredUrl?
    suspend fun findAllActive(): List<MonitoredUrl>
    suspend fun findByOwner(ownerId: UserId): List<MonitoredUrl>
    suspend fun save(url: MonitoredUrl): MonitoredUrl
    suspend fun delete(id: UrlId)
}
