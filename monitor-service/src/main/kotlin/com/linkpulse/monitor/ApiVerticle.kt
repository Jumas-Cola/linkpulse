package com.linkpulse.monitor

import com.linkpulse.domain.model.UserId
import com.linkpulse.domain.port.UrlRepository
import com.linkpulse.domain.port.auth.UserLoginer
import com.linkpulse.domain.port.auth.UserRegistrar
import com.linkpulse.monitor.adapter.input.http.AuthHandler
import com.linkpulse.monitor.adapter.input.http.UrlsHandler
import com.linkpulse.monitor.adapter.input.http.putUserId
import io.github.oshai.kotlinlogging.KotlinLogging
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.JWTAuthHandler
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.coAwait
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private val logger = KotlinLogging.logger {}

class ApiVerticle : CoroutineVerticle(), KoinComponent {

    private val userRegistrar: UserRegistrar by inject()
    private val userLoginer: UserLoginer by inject()
    private val jwtAuth: JWTAuth by inject()
    private val urlRepository: UrlRepository by inject()

    override suspend fun start() {
        val router = Router.router(vertx)

        router.route().handler(BodyHandler.create())

        // ── Health check ──
        router.get("/health").handler { ctx ->
            ctx.response()
                .putHeader("Content-Type", "application/json")
                .end("""{"status":"UP"}""")
        }

        // ── Auth (публичные маршруты — до JWT middleware) ──
        val authHandler = AuthHandler(userRegistrar, userLoginer, jwtAuth, this)
        router.post("/api/auth/register").handler(authHandler::register)
        router.post("/api/auth/login").handler(authHandler::login)

        // ── JWT middleware (защищает все /api/* маршруты ниже) ──
        router.route("/api/*")
            .handler(JWTAuthHandler.create(jwtAuth))
            .handler { ctx ->
                ctx.putUserId(UserId(ctx.user()!!.principal().getString("sub").toLong()))
                ctx.next()
            }

        // ── URL routes ──
        val urlsHandler = UrlsHandler(urlRepository, this)
        router.post("/api/urls").handler(urlsHandler::createUrl)
        router.get("/api/urls").handler(urlsHandler::listUrls)
        router.get("/api/urls/:id").handler(urlsHandler::getUrl)
        router.delete("/api/urls/:id").handler(urlsHandler::deleteUrl)

        vertx.createHttpServer()
            .requestHandler(router)
            .listen(8080)
            .coAwait()

        logger.info { "HTTP server listening on port 8080" }
    }
}
