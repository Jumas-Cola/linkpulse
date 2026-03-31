package com.linkpulse.domain.port

import com.linkpulse.domain.model.CheckResult
import com.linkpulse.domain.model.UrlId

interface CheckResultRepository {
    suspend fun save(urlId: UrlId, result: CheckResult)
    suspend fun findByUrlId(urlId: UrlId, limit: Int = 50, offset: Int = 0): List<CheckResult>
}
