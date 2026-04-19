package com.linkpulse.monitor.adapter.input.http

import com.linkpulse.domain.model.CheckResult
import com.linkpulse.domain.model.MonitoredUrl
import com.linkpulse.domain.model.UrlId
import com.linkpulse.domain.model.UrlStatus
import com.linkpulse.domain.model.UserId
import com.linkpulse.domain.service.MonitoringService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.kotlin.coroutines.coAwait
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class UrlsHandlerTest : FunSpec({

    val vertx = Vertx.vertx()
    val service = mockk<MonitoringService>()
    val handler = UrlsHandler(CoroutineScope(Dispatchers.Default), service)

    lateinit var client: WebClient
    var port = 0

    val testUserId = UserId(1L)

    fun testUrl(id: Long = 1L) = MonitoredUrl(
        id = UrlId(id),
        url = "https://example.com",
        name = "Example",
        intervalSeconds = 60,
        owner = testUserId,
    )

    beforeSpec {
        val router = Router.router(vertx)

        router.route().handler(BodyHandler.create())

        // Fake JWT middleware — имитирует то, что делает JwtAuthHandler
        router.route("/api/*").handler { ctx ->
            ctx.putUserId(testUserId)
            ctx.next()
        }

        router.get("/api/urls").handler(handler::listUrls)
        router.post("/api/urls").handler(handler::createUrl)
        router.get("/api/urls/:urlId").handler(handler::getUrl)
        router.delete("/api/urls/:urlId").handler(handler::deleteUrl)
        router.get("/api/urls/:urlId/history").handler(handler::getUrlHistory)

        val server = vertx.createHttpServer()
            .requestHandler(router)
            .listen(0)
            .coAwait()

        port = server.actualPort()
        client = WebClient.create(vertx)
    }

    afterSpec {
        vertx.close().coAwait()
    }

    beforeEach { clearMocks(service) }

    // ── GET /api/urls ──────────────────────────────────────────────

    test("GET /api/urls returns 200 with list of urls") {
        coEvery { service.getUrlsByOwner(testUserId) } returns listOf(testUrl())

        val response = client.get(port, "localhost", "/api/urls").send().coAwait()

        response.statusCode() shouldBe 200
        coVerify(exactly = 1) { service.getUrlsByOwner(testUserId) }
    }

    test("GET /api/urls returns 200 with empty list when owner has no urls") {
        coEvery { service.getUrlsByOwner(testUserId) } returns emptyList()

        val response = client.get(port, "localhost", "/api/urls").send().coAwait()

        response.statusCode() shouldBe 200
        response.bodyAsString() shouldBe "[]"
    }

    // ── POST /api/urls ─────────────────────────────────────────────

    test("POST /api/urls returns 201 on success") {
        coEvery { service.addUrl(any(), any(), any(), testUserId) } returns testUrl()

        val response = client.post(port, "localhost", "/api/urls")
            .putHeader("Content-Type", "application/json")
            .sendBuffer(io.vertx.core.buffer.Buffer.buffer("""{"url":"https://example.com","intervalSeconds":60}"""))
            .coAwait()

        response.statusCode() shouldBe 201
        coVerify(exactly = 1) { service.addUrl("https://example.com", any(), 60, testUserId) }
    }

    test("POST /api/urls returns 400 when body is missing") {
        val response = client.post(port, "localhost", "/api/urls")
            .putHeader("Content-Type", "application/json")
            .send()
            .coAwait()

        response.statusCode() shouldBe 400
    }

    test("POST /api/urls returns 400 when body is malformed JSON") {
        val response = client.post(port, "localhost", "/api/urls")
            .putHeader("Content-Type", "application/json")
            .sendBuffer(io.vertx.core.buffer.Buffer.buffer("not-json"))
            .coAwait()

        response.statusCode() shouldBe 400
    }

    test("POST /api/urls returns 400 when service rejects url") {
        coEvery { service.addUrl(any(), any(), any(), testUserId) } throws
                IllegalArgumentException("URL must start with http:// or https://")

        val response = client.post(port, "localhost", "/api/urls")
            .putHeader("Content-Type", "application/json")
            .sendBuffer(io.vertx.core.buffer.Buffer.buffer("""{"url":"ftp://bad","intervalSeconds":60}"""))
            .coAwait()

        response.statusCode() shouldBe 400
    }

    // ── GET /api/urls/{id} ─────────────────────────────────────────

    test("GET /api/urls/{id} returns 200 with url details") {
        coEvery { service.getUrlDetails(UrlId(42L), testUserId) } returns testUrl(id = 42L)

        val response = client.get(port, "localhost", "/api/urls/42").send().coAwait()

        response.statusCode() shouldBe 200
        coVerify(exactly = 1) { service.getUrlDetails(UrlId(42L), testUserId) }
    }

    test("GET /api/urls/{id} returns 400 when id is not a number") {
        val response = client.get(port, "localhost", "/api/urls/abc").send().coAwait()

        response.statusCode() shouldBe 400
    }

    test("GET /api/urls/{id} returns 404 when url not found") {
        coEvery { service.getUrlDetails(UrlId(999L), testUserId) } throws
                NoSuchElementException("URL with id=999 not found")

        val response = client.get(port, "localhost", "/api/urls/999").send().coAwait()

        response.statusCode() shouldBe 404
    }

    // ── DELETE /api/urls/{id} ──────────────────────────────────────

    test("DELETE /api/urls/{id} returns 200 on success") {
        coEvery { service.removeUrl(UrlId(42L), testUserId) } returns Unit

        val response = client.delete(port, "localhost", "/api/urls/42").send().coAwait()

        response.statusCode() shouldBe 200
        coVerify(exactly = 1) { service.removeUrl(UrlId(42L), testUserId) }
    }

    test("DELETE /api/urls/{id} returns 400 when id is not a number") {
        val response = client.delete(port, "localhost", "/api/urls/abc").send().coAwait()

        response.statusCode() shouldBe 400
    }

    test("DELETE /api/urls/{id} returns 400 when access denied") {
        coEvery { service.removeUrl(UrlId(42L), testUserId) } throws
                IllegalArgumentException("Access denied")

        val response = client.delete(port, "localhost", "/api/urls/42").send().coAwait()

        response.statusCode() shouldBe 400
    }

    // ── GET /api/urls/{id}/history ─────────────────────────────────

    test("GET /api/urls/{id}/history returns 200 with check results") {
        val result = CheckResult(httpStatus = 200, latencyMs = 42, error = null)
        coEvery { service.getCheckHistory(UrlId(1L), 50, 0) } returns listOf(result)

        val response = client.get(port, "localhost", "/api/urls/1/history").send().coAwait()

        response.statusCode() shouldBe 200
        coVerify(exactly = 1) { service.getCheckHistory(UrlId(1L), 50, 0) }
    }

    test("GET /api/urls/{id}/history returns 200 with empty list when no history") {
        coEvery { service.getCheckHistory(UrlId(1L), 50, 0) } returns emptyList()

        val response = client.get(port, "localhost", "/api/urls/1/history").send().coAwait()

        response.statusCode() shouldBe 200
        response.bodyAsString() shouldBe "[]"
    }

    test("GET /api/urls/{id}/history passes limit and offset from query params") {
        coEvery { service.getCheckHistory(UrlId(1L), 10, 20) } returns emptyList()

        val response = client.get(port, "localhost", "/api/urls/1/history?limit=10&offset=20")
            .send().coAwait()

        response.statusCode() shouldBe 200
        coVerify(exactly = 1) { service.getCheckHistory(UrlId(1L), 10, 20) }
    }

    test("GET /api/urls/{id}/history returns 400 when id is not a number") {
        val response = client.get(port, "localhost", "/api/urls/abc/history").send().coAwait()

        response.statusCode() shouldBe 400
    }

    test("GET /api/urls/{id}/history returns 400 when limit is not a number") {
        val response = client.get(port, "localhost", "/api/urls/1/history?limit=abc")
            .send().coAwait()

        response.statusCode() shouldBe 400
    }
})
