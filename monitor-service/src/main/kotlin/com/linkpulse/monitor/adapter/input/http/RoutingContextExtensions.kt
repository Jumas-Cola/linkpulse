package com.linkpulse.monitor.adapter.input.http

import com.linkpulse.domain.model.UserId
import io.vertx.ext.web.RoutingContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val USER_ID_KEY = "userId"

fun RoutingContext.userId(): UserId =
    get(USER_ID_KEY) ?: error("UserId not found in context — is the JWT middleware applied?")

fun RoutingContext.putUserId(userId: UserId) = put(USER_ID_KEY, userId)

fun RoutingContext.sendJson(status: Int, body: String) {
    response().setStatusCode(status).putHeader("Content-Type", "application/json").end(body)
}

inline fun <reified T> RoutingContext.sendJson(status: Int, body: T) =
    sendJson(status, Json.encodeToString(body))
