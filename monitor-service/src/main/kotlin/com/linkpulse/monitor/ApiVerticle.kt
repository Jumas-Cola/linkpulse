package com.linkpulse.monitor

import io.github.oshai.kotlinlogging.KotlinLogging
import io.vertx.ext.web.Router
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.coAwait

private val logger = KotlinLogging.logger {}

class ApiVerticle : CoroutineVerticle() {

    override suspend fun start() {
        val router = Router.router(vertx)

        // ── Health check ──
        router.get("/health").handler { ctx ->
            ctx.response()
                .putHeader("Content-Type", "application/json")
                .end("""{"status":"UP"}""")
        }

        // ── API placeholder ──
        router.get("/api/urls").handler { ctx ->
            ctx.response()
                .putHeader("Content-Type", "application/json")
                .end("""{"message":"TODO: implement URL listing"}""")
        }

        vertx.createHttpServer()
            .requestHandler(router)
            .listen(8080)
            .coAwait()

        logger.info { "HTTP server listening on port 8080" }
    }
}
