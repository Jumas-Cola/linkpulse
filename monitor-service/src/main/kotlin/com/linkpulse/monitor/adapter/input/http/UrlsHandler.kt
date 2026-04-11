package com.linkpulse.monitor.adapter.input.http

import com.linkpulse.domain.model.MonitoredUrl
import com.linkpulse.domain.model.UrlId
import com.linkpulse.domain.port.UrlRepository
import com.linkpulse.monitor.adapter.input.http.dto.request.UrlRequest
import com.linkpulse.monitor.adapter.input.http.dto.response.UrlResponse
import io.vertx.ext.web.RoutingContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class UrlsHandler(
    private val urlRepository: UrlRepository,
    private val scope: CoroutineScope
) {
    fun createUrl(ctx: RoutingContext) {
        scope.launch {
            try {
                val req = Json.decodeFromString<UrlRequest>(
                    ctx.body().asString() ?: throw IllegalArgumentException("Request body is missing")
                )

                val url = urlRepository.save(
                    MonitoredUrl(
                        url = req.url,
                        name = req.name ?: req.url.substringAfterLast("/"),
                        intervalSeconds = req.intervalSeconds,
                        owner = ctx.userId()
                    )
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

                val urls = urlRepository.findByOwner(userId)

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

                val url = urlRepository.findByIdAndOwner(
                    urlId,
                    userId
                )

                if (url == null) {
                    ctx.sendJson(404, errorBody("Invalid URL body"))
                }

                ctx.sendJson(200, UrlResponse.from(url!!))
            } catch (e: SerializationException) {
                ctx.sendJson(400, errorBody("Invalid JSON body"))
            } catch (e: IllegalArgumentException) {
                ctx.sendJson(400, errorBody(e.message))
            } catch (e: IllegalStateException) {
                ctx.sendJson(409, errorBody(e.message))
            }
        }
    }

    fun deleteUrl(ctx: RoutingContext) {
        TODO()
    }

    private fun errorBody(message: String?) = """{"error":${Json.encodeToString(message ?: "Unknown error")}}"""
}
