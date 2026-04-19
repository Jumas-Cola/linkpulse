package com.linkpulse.domain.service

import com.linkpulse.domain.model.*
import com.linkpulse.domain.port.CheckResultRepository
import com.linkpulse.domain.port.UrlRepository

class MonitoringService(
    private val urlRepo: UrlRepository,
    private val checkResultRepo: CheckResultRepository
) {
    suspend fun addUrl(url: String, name: String, intervalSeconds: Int, owner: UserId): MonitoredUrl {
        require(url.startsWith("http://") || url.startsWith("https://")) {
            "URL must start with http:// or https://"
        }
        require(intervalSeconds in 10..3600) {
            "Interval must be between 10 and 3600 seconds"
        }
        val entity = MonitoredUrl(
            url = url,
            name = name.ifBlank { url },
            intervalSeconds = intervalSeconds,
            owner = owner
        )
        return urlRepo.save(entity)
    }

    suspend fun removeUrl(id: UrlId, owner: UserId) {
        val existing = urlRepo.findById(id)
            ?: throw NoSuchElementException("URL with id=${id.value} not found")
        require(existing.owner == owner) { "Access denied" }
        urlRepo.delete(id)
    }

    suspend fun getUrlsByOwner(owner: UserId): List<MonitoredUrl> =
        urlRepo.findByOwner(owner)

    suspend fun getUrlDetails(id: UrlId, owner: UserId): MonitoredUrl {
        val url = urlRepo.findById(id)
            ?: throw NoSuchElementException("URL with id=${id.value} not found")
        require(url.owner == owner) { "Access denied" }
        return url
    }

    suspend fun getCheckHistory(urlId: UrlId, limit: Int = 50, offset: Int = 0): List<CheckResult> =
        checkResultRepo.findByUrlId(urlId, limit, offset)
}
