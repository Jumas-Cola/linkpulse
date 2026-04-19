package com.linkpulse.monitor.adapter.input.http

import com.linkpulse.domain.model.UrlId
import com.linkpulse.domain.service.MonitoringService
import com.linkpulse.monitor.adapter.input.http.dto.request.UrlRequest
import com.linkpulse.monitor.adapter.input.http.dto.response.CheckResultResponse
import com.linkpulse.monitor.adapter.input.http.dto.response.UrlResponse
import io.vertx.ext.web.RoutingContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class UrlsHandler(
    private val scope: CoroutineScope,
    private val service: MonitoringService
) {
    fun createUrl(ctx: RoutingContext) {
        scope.launch {
            try {
                val req = Json.decodeFromString<UrlRequest>(
                    ctx.body().asString() ?: throw IllegalArgumentException("Request body is missing")
                )

                val url = service.addUrl(
                    url = req.url,
                    name = req.name ?: req.url.substringAfterLast("/"),
                    intervalSeconds = req.intervalSeconds,
                    owner = ctx.userId()
                )

                ctx.sendJson(201, UrlResponse.from(url))
            } catch (e: SerializationException) {
                ctx.sendJson(400, errorBody("Invalid JSON body"))
            } catch (e: IllegalArgumentException) {
                ctx.sendJson(400, errorBody(e.message))
            } catch (e: IllegalStateException) {
                ctx.sendJson(409, errorBody(e.message))
            }
        }
    }

    fun listUrls(ctx: RoutingContext) {
        scope.launch {
            try {
                val userId = ctx.userId()

                val urls = service.getUrlsByOwner(userId)

                ctx.sendJson(200, urls.map { UrlResponse.from(it) })
            } catch (e: SerializationException) {
                ctx.sendJson(400, errorBody("Invalid JSON body"))
            } catch (e: IllegalArgumentException) {
                ctx.sendJson(400, errorBody(e.message))
            } catch (e: IllegalStateException) {
                ctx.sendJson(409, errorBody(e.message))
            }
        }
    }

    fun getUrl(ctx: RoutingContext) {
        scope.launch {
            try {
                val userId = ctx.userId()

                val urlId = UrlId(
                    ctx.pathParam("urlId")?.toLong() ?: throw IllegalArgumentException("urlId is required")
                )

                val url = service.getUrlDetails(urlId, userId)

                ctx.sendJson(200, UrlResponse.from(url))
            } catch (e: SerializationException) {
                ctx.sendJson(400, errorBody("Invalid JSON body"))
            } catch (e: IllegalArgumentException) {
                ctx.sendJson(400, errorBody(e.message))
            } catch (e: NoSuchElementException) {
                ctx.sendJson(404, errorBody(e.message))
            } catch (e: IllegalStateException) {
                ctx.sendJson(409, errorBody(e.message))
            }
        }
    }

    fun deleteUrl(ctx: RoutingContext) {
        scope.launch {
            try {
                val userId = ctx.userId()

                val urlId = UrlId(
                    ctx.pathParam("urlId")?.toLong() ?: throw IllegalArgumentException("urlId is required")
                )

                service.removeUrl(
                    urlId,
                    userId
                )

                ctx.sendJson(200, okBody())
            } catch (e: SerializationException) {
                ctx.sendJson(400, errorBody("Invalid JSON body"))
            } catch (e: IllegalArgumentException) {
                ctx.sendJson(400, errorBody(e.message))
            } catch (e: NoSuchElementException) {
                ctx.sendJson(404, errorBody(e.message))
            } catch (e: IllegalStateException) {
                ctx.sendJson(409, errorBody(e.message))
            }
        }
    }

    fun getUrlHistory(ctx: RoutingContext) {
        scope.launch {
            try {
                val urlId = UrlId(
                    ctx.pathParam("urlId")?.toLong() ?: throw IllegalArgumentException("urlId is required")
                )

                val limit = ctx.queryParam("limit").firstOrNull()?.toInt() ?: 50
                val offset = ctx.queryParam("offset").firstOrNull()?.toInt() ?: 0

                val history = service.getCheckHistory(
                    urlId,
                    limit,
                    offset,
                )

                ctx.sendJson(200, history.map { CheckResultResponse.from(it) })
            } catch (e: SerializationException) {
                ctx.sendJson(400, errorBody("Invalid JSON body"))
            } catch (e: IllegalArgumentException) {
                ctx.sendJson(400, errorBody(e.message))
            } catch (e: NoSuchElementException) {
                ctx.sendJson(404, errorBody(e.message))
            } catch (e: IllegalStateException) {
                ctx.sendJson(409, errorBody(e.message))
            }
        }
    }

    private fun errorBody(message: String?) = """{"error":${Json.encodeToString(message ?: "Unknown error")}}"""

    private fun okBody() = """{"error": "OK"}"""
}
