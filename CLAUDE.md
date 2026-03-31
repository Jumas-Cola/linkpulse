# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build all modules
./gradlew build

# Run tests (all modules)
./gradlew test

# Run tests for a single module
./gradlew :domain:test
./gradlew :monitor-service:test
./gradlew :notification-service:test

# Run a single test class
./gradlew :domain:test --tests "dev.starlightx.linkpulse.domain.service.CheckOrchestratorTest"

# Start PostgreSQL (required before running services)
docker compose up -d postgres

# Run monitor-service locally
./gradlew :monitor-service:run

# Build fat JARs for deployment
./gradlew :monitor-service:fatJar
./gradlew :notification-service:fatJar
```

## Architecture

LinkPulse is a URL monitoring service using a **multi-module Gradle project** with Vert.x 5 and Kotlin, following clean/hexagonal architecture. JDK 21 is required.

### Modules

- **`domain/`** — Pure business logic with no framework dependencies. Defines ports (interfaces), domain events, value objects, entities, and use cases. This module must stay framework-free.
- **`monitor-service/`** — HTTP API server, health check scheduler, and PostgreSQL persistence. Implements domain ports as adapters.
- **`notification-service/`** — Listens on the Vert.x Event Bus for domain events and sends notifications. Shares only the `domain` module with monitor-service.

Dependency graph: `notification-service → domain ← monitor-service`

### Domain Layer (`domain/`)

The core logic lives here:

- `model/` — Value objects (`UrlId`, `UserId`), `MonitoredUrl` entity, `CheckResult`, `UrlStatus` enum
- `event/` — Sealed `DomainEvent` interface with `UrlWentDown` and `UrlRecovered` implementations (kotlinx.serialization for Event Bus transport)
- `port/` — `UrlRepository`, `HealthChecker`, `CheckResultRepository`, `EventPublisher` interfaces (all suspend functions)
- `service/` — `MonitoringService` (URL CRUD), `CheckOrchestrator` (runs all checks with max 20 concurrent via Semaphore, publishes domain events on status transitions)

Status transition logic is in `MonitoredUrl.applyCheck()` — it returns the updated entity and an optional `DomainEvent` to publish.

### Monitor Service (`monitor-service/`)

- `Main.kt` — Initializes Vert.x with native transport, loads HOCON config (`application.conf`), runs Flyway migrations, deploys verticles
- `ApiVerticle.kt` — CoroutineVerticle serving HTTP on port 8080; uses `coAwait()` to bridge Vert.x Futures with suspend functions
- `adapter/output/persistence/` — Exposed ORM tables (`UsersTable`, `MonitoredUrlsTable`, `CheckResultsTable`) and `ExposedUrlRepository`
- `resources/db/migration/` — Flyway SQL migrations (naming: `V001__...`, `V002__...`)
- `resources/application.conf` — HOCON config with environment variable overrides via `${?VAR}` syntax

### Notification Service (`notification-service/`)

- `EventListenerVerticle.kt` — Subscribes to `events.url.down` and `events.url.recovered` on the clustered Event Bus (Hazelcast), deserializes events, handles notifications

### Key Technologies

| Purpose | Library |
|---------|---------|
| HTTP / Event Bus | Vert.x 5 |
| Async | kotlinx-coroutines (CoroutineVerticle) |
| Serialization | kotlinx-serialization |
| ORM | Jetbrains Exposed |
| Migrations | Flyway |
| Clustering | Hazelcast |
| Testing | Kotest (FunSpec) + MockK + Testcontainers |

### Configuration

`monitor-service/src/main/resources/application.conf` holds all defaults. Override at runtime with environment variables:
- `DB_URL`, `DB_USER`, `DB_PASSWORD`
- `JWT_SECRET`
- `CHECKER_INTERVAL_MS`, `CHECKER_TIMEOUT_MS`, `CHECKER_MAX_CONCURRENCY`