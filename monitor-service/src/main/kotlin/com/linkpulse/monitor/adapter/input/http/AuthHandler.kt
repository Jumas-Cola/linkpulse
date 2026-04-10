package com.linkpulse.monitor.adapter.input.http

import com.linkpulse.domain.port.auth.UserLoginer
import com.linkpulse.domain.port.auth.UserRegistrar
import com.linkpulse.monitor.adapter.input.http.dto.request.LoginRequest
import com.linkpulse.monitor.adapter.input.http.dto.request.RegisterRequest
import com.linkpulse.monitor.adapter.input.http.dto.response.AuthResponse
import com.linkpulse.monitor.adapter.input.http.dto.response.RegisterResponse
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.JWTOptions
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.web.RoutingContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
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
                ctx.sendJson(201, RegisterResponse(user.id!!.value.toString(), user.username))
            } catch (e: SerializationException) {
                ctx.sendJson(400, errorBody("Invalid JSON body"))
            } catch (e: IllegalArgumentException) {
                ctx.sendJson(400, errorBody(e.message))
            } catch (e: IllegalStateException) {
                ctx.sendJson(409, errorBody(e.message))
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
                    JsonObject().put("sub", user.id!!.value.toString()).put("username", user.username),
                    JWT_OPTIONS
                )
                ctx.sendJson(200, AuthResponse(user.id!!.value.toString(), user.username, token))
            } catch (e: SerializationException) {
                ctx.sendJson(400, errorBody("Invalid JSON body"))
            } catch (e: IllegalArgumentException) {
                ctx.sendJson(400, errorBody(e.message))
            } catch (e: NoSuchElementException) {
                ctx.sendJson(401, errorBody(e.message))
            } catch (e: IllegalStateException) {
                ctx.sendJson(401, errorBody(e.message))
            }
        }
    }

    private fun errorBody(message: String?) = """{"error":${Json.encodeToString(message ?: "Unknown error")}}"""

    companion object {
        private val JWT_OPTIONS = JWTOptions().setExpiresInMinutes(60)
    }
}
