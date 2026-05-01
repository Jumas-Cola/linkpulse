package com.linkpulse.monitor

import com.linkpulse.domain.model.UserId
import com.linkpulse.domain.port.auth.UserLoginer
import com.linkpulse.domain.port.auth.UserRegistrar
import com.linkpulse.domain.service.MonitoringService
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
    private val monitoringService: MonitoringService by inject()

    override suspend fun start() {
        val router = Router.router(vertx)

        router.route().handler(BodyHandler.create())

        // ── Serve OpenAPI spec ──
        router.get("/openapi.yaml").handler { ctx ->
            ctx.response()
                .putHeader("Content-Type", "application/yaml")
                .sendFile("openapi.yaml")
        }

        // ── Swagger UI ──
        router.get("/swagger-ui").handler { ctx ->
            ctx.response()
                .putHeader("Content-Type", "text/html; charset=utf-8")
                .end("""
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <title>LinkPulse API - Swagger UI</title>
                        <link rel="stylesheet" type="text/css" href="https://unpkg.com/swagger-ui-dist@5.11.0/swagger-ui.css">
                    </head>
                    <body>
                        <div id="swagger-ui"></div>
                        <script src="https://unpkg.com/swagger-ui-dist@5.11.0/swagger-ui-bundle.js"></script>
                        <script>
                            SwaggerUIBundle({
                                url: '/openapi.yaml',
                                dom_id: '#swagger-ui'
                            });
                        </script>
                    </body>
                    </html>
                """.trimIndent())
        }

        // ── Health check ──
        router.get("/health").handler { ctx ->
            ctx.response()
                .putHeader("Content-Type", "application/json")
                .end("""{"status":"UP"}""")
        }

        // ── Auth (public routes) ──
        val authHandler = AuthHandler(userRegistrar, userLoginer, jwtAuth, this)
        router.post("/api/auth/register").handler(authHandler::register)
        router.post("/api/auth/login").handler(authHandler::login)

        // ── JWT middleware (protects /api/* below) ──
        router.route("/api/*")
            .handler(JWTAuthHandler.create(jwtAuth))
            .handler { ctx ->
                ctx.putUserId(UserId(ctx.user()!!.principal().getString("sub").toLong()))
                ctx.next()
            }

        // ── URL routes ──
        val urlsHandler = UrlsHandler(this, monitoringService)
        router.get("/api/urls/:urlId").handler(urlsHandler::getUrl)
        router.delete("/api/urls/:urlId").handler(urlsHandler::deleteUrl)
        router.get("/api/urls").handler(urlsHandler::listUrls)
        router.post("/api/urls").handler(urlsHandler::createUrl)
        router.get("/api/urls/history/:urlId").handler(urlsHandler::getUrlHistory)

        vertx.createHttpServer()
            .requestHandler(router)
            .listen(8080)
            .coAwait()

        logger.info { "HTTP server listening on port 8080" }
        logger.info { "Swagger UI available at http://localhost:8080/swagger-ui" }
    }
}
