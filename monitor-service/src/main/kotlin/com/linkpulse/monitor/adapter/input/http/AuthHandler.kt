package com.linkpulse.monitor.adapter.input.http

import com.linkpulse.domain.port.auth.UserLoginer
import com.linkpulse.domain.port.auth.UserRegistrar
import com.linkpulse.monitor.adapter.input.http.dto.request.LoginRequest
import com.linkpulse.monitor.adapter.input.http.dto.request.RegisterRequest
import com.linkpulse.monitor.adapter.input.http.dto.response.AuthResponse
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.JWTOptions
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.web.RoutingContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AuthHandler(
    private val userRegistrar: UserRegistrar,
    private val userLoginer: UserLoginer,
    private val jwtAuth: JWTAuth,
    private val scope: CoroutineScope
) {
    fun register(ctx: RoutingContext) {
        scope.launch {
            try {
                val req = Json.decodeFromString<RegisterRequest>(
                    ctx.body().asString() ?: throw IllegalArgumentException("Request body is missing")
                )
                val user = userRegistrar.register(req.username, req.password)
                ctx.response()
                    .setStatusCode(201)
                    .putHeader("Content-Type", "application/json")
                    .end(JsonObject().put("id", user.id.value).put("username", user.username).encode())
            } catch (e: SerializationException) {
                ctx.response()
                    .setStatusCode(400)
                    .putHeader("Content-Type", "application/json")
                    .end(JsonObject().put("error", "Invalid JSON body").encode())
            } catch (e: IllegalArgumentException) {
                ctx.response()
                    .setStatusCode(400)
                    .putHeader("Content-Type", "application/json")
                    .end(JsonObject().put("error", e.message).encode())
            } catch (e: IllegalStateException) {
                ctx.response()
                    .setStatusCode(409)
                    .putHeader("Content-Type", "application/json")
                    .end(JsonObject().put("error", e.message).encode())
            }
        }
    }

    fun login(ctx: RoutingContext) {
        scope.launch {
            try {
                val req = Json.decodeFromString<LoginRequest>(
                    ctx.body().asString() ?: throw IllegalArgumentException("Request body is missing")
                )
                val user = userLoginer.login(req.username, req.password)
                val token = jwtAuth.generateToken(
                    JsonObject().put("sub", user.id.value.toString()).put("username", user.username),
                    JWTOptions().setExpiresInMinutes(60)
                )
                ctx.response()
                    .setStatusCode(200)
                    .putHeader("Content-Type", "application/json")
                    .end(Json.encodeToString(AuthResponse(token)))
            } catch (e: SerializationException) {
                ctx.response()
                    .setStatusCode(400)
                    .putHeader("Content-Type", "application/json")
                    .end(JsonObject().put("error", "Invalid JSON body").encode())
            } catch (e: IllegalArgumentException) {
                ctx.response()
                    .setStatusCode(400)
                    .putHeader("Content-Type", "application/json")
                    .end(JsonObject().put("error", e.message).encode())
            } catch (e: NoSuchElementException) {
                ctx.response()
                    .setStatusCode(401)
                    .putHeader("Content-Type", "application/json")
                    .end(JsonObject().put("error", e.message).encode())
            } catch (e: IllegalStateException) {
                ctx.response()
                    .setStatusCode(401)
                    .putHeader("Content-Type", "application/json")
                    .end(JsonObject().put("error", e.message).encode())
            }
        }
    }
}
