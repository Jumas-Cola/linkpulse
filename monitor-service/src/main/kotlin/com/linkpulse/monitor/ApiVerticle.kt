package com.linkpulse.monitor

import io.github.oshai.kotlinlogging.KotlinLogging
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
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

        // ── API routes ──
        router.get("/api/urls").handler { ctx ->
            val endpointsArray = JsonArray()

            for (route in router.routes) {
                val path = route.path
                if (path != null && route.methods().isNotEmpty()) {
                    val methodsArray = JsonArray()
                    for (method in route.methods()) {
                        methodsArray.add(method.name())
                    }

                    val endpointObj = JsonObject()
                    endpointObj.put("methods", methodsArray)
                    endpointObj.put("path", path)

                    endpointsArray.add(endpointObj)
                }
            }

            val resultObj = JsonObject()
            resultObj.put("endpoints", endpointsArray)
            resultObj.put("total", endpointsArray.size())

            ctx.response()
                .putHeader("Content-Type", "application/json")
                .end(resultObj.encode())
        }

        vertx.createHttpServer()
            .requestHandler(router)
            .listen(8080)
            .coAwait()

        logger.info { "HTTP server listening on port 8080" }
    }
}
