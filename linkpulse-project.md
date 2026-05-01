# 📦 Учебный проект: LinkPulse — Сервис мониторинга URL

> Два Vert.x-сервиса, которые периодически проверяют доступность URL-адресов,
> собирают метрики отклика и уведомляют пользователя при даунтайме.
> Фокус — корутины, Clean Architecture, межсервисное взаимодействие через Event Bus.

---

## Зачем именно этот проект

| Навык из roadmap | Как покрывается |
|-----------------|-----------------|
| Корутины и structured concurrency | Параллельные health-check'и десятков URL с контролем таймаутов и отменой |
| Flow | Стрим метрик из планировщика → агрегация → запись в БД |
| Clean Architecture | Слои domain → use cases → adapters → infrastructure, чёткие boundaries |
| DDD-lite | Bounded Contexts (Monitoring, Notification), Value Objects, Domain Events |
| Vert.x Event Bus | Асинхронное общение между Verticle'ами и между сервисами |
| Vert.x Coroutine Integration | `CoroutineVerticle`, `coHandler`, `awaitResult`, suspend-обёртки |
| PostgreSQL + Exposed | Хранение URL, истории проверок, пользователей |
| JWT Auth | Уже знакомо — закрепить в новом контексте |
| Тестирование | Kotest + Testcontainers для интеграционных тестов с реальной БД |
| Docker Compose | Два сервиса + PostgreSQL + (опционально) Redis в одной сети |

---

## Архитектура

```
┌─────────────────────────────────────────────────────────┐
│                    Docker Compose                        │
│                                                         │
│  ┌─────────────────────┐    ┌────────────────────────┐  │
│  │  monitor-service     │    │  notification-service  │  │
│  │                     │    │                        │  │
│  │  ┌───────────────┐  │    │  ┌──────────────────┐  │  │
│  │  │ API Verticle  │  │    │  │ Listener Verticle│  │  │
│  │  │ (REST + JWT)  │  │    │  │ (Event Bus)      │  │  │
│  │  └───────┬───────┘  │    │  └────────┬─────────┘  │  │
│  │          │          │    │           │            │  │
│  │  ┌───────┴───────┐  │    │  ┌────────┴─────────┐  │  │
│  │  │ Scheduler     │  │    │  │ Notifier         │  │  │
│  │  │ Verticle      │──────────│ (Telegram/Email)  │  │  │
│  │  │ (cron checks) │ Event │  │                  │  │  │
│  │  └───────┬───────┘  Bus  │  └──────────────────┘  │  │
│  │          │          │    │                        │  │
│  │  ┌───────┴───────┐  │    └────────────────────────┘  │
│  │  │ Checker       │  │                                │
│  │  │ Verticle      │  │                                │
│  │  │ (HTTP probes) │  │                                │
│  │  └───────────────┘  │                                │
│  └──────────┬──────────┘                                │
│             │                                           │
│       ┌─────┴─────┐                                     │
│       │ PostgreSQL │                                     │
│       └───────────┘                                     │
└─────────────────────────────────────────────────────────┘
```

---

## Clean Architecture — структура модулей

```
linkpulse/
├── domain/                          # Gradle-модуль: чистое ядро
│   └── src/main/kotlin/.../domain/
│       ├── model/                   # MonitoredUrl, CheckResult, UrlStatus, UrlId, UserId
│       ├── event/                   # DomainEvent, UrlWentDown, UrlRecovered
│       ├── port/                    # UrlRepository, HealthChecker, EventPublisher...
│       └── service/                 # CheckOrchestrator, MonitoringService
│
├── monitor-service/                 # Gradle-модуль: depends on :domain
│   └── src/main/kotlin/.../monitor/
│       ├── adapter/
│       │   ├── input/rest/          # UrlController, AuthHandler, dto/
│       │   ├── input/scheduler/     # CheckSchedulerVerticle
│       │   └── output/
│       │       ├── persistence/     # ExposedUrlRepository, tables/
│       │       ├── http/            # VertxHealthChecker
│       │       └── eventbus/        # EventBusPublisher
│       ├── config/                  # AppConfig, DI
│       ├── ApiVerticle.kt
│       └── Main.kt
│
└── notification-service/            # Gradle-модуль: depends on :domain
    └── src/main/kotlin/.../notification/
        ├── adapter/
        │   ├── input/               # EventBusListener
        │   └── output/              # TelegramSender, LogSender
        ├── EventListenerVerticle.kt
        └── Main.kt

```

---

## Предметная область (Domain)

### Entities и Value Objects

```kotlin
// domain/model/MonitoredUrl.kt
data class MonitoredUrl(
    val id: UrlId,
    val url: String,
    val name: String,
    val intervalSeconds: Int,        // как часто проверять
    val owner: UserId,
    val currentStatus: UrlStatus,
    val consecutiveFailures: Int,
    val createdAt: Instant
) {
    fun applyCheck(result: CheckResult): Pair<MonitoredUrl, DomainEvent?> {
        val newStatus = result.deriveStatus()
        val event = detectTransition(currentStatus, newStatus)
        return copy(
            currentStatus = newStatus,
            consecutiveFailures = if (newStatus == UrlStatus.UP) 0
                                  else consecutiveFailures + 1
        ) to event
    }

    private fun detectTransition(old: UrlStatus, new: UrlStatus): DomainEvent? =
        when {
            old == UrlStatus.UP && new == UrlStatus.DOWN ->
                UrlWentDown(id, url, Instant.now())
            old == UrlStatus.DOWN && new == UrlStatus.UP ->
                UrlRecovered(id, url, Instant.now())
            else -> null
        }
}

@JvmInline value class UrlId(val value: Long)
@JvmInline value class UserId(val value: Long)

enum class UrlStatus { UP, DOWN, DEGRADED, UNKNOWN }

data class CheckResult(
    val httpStatus: Int?,
    val latencyMs: Long,
    val error: String?,
    val checkedAt: Instant
) {
    fun deriveStatus(): UrlStatus = when {
        error != null        -> UrlStatus.DOWN
        httpStatus in 200..399 -> if (latencyMs > 5000) UrlStatus.DEGRADED else UrlStatus.UP
        else                 -> UrlStatus.DOWN
    }
}
```

### Domain Events

```kotlin
sealed interface DomainEvent {
    val urlId: UrlId
    val occurredAt: Instant
}

data class UrlWentDown(
    override val urlId: UrlId,
    val url: String,
    override val occurredAt: Instant
) : DomainEvent

data class UrlRecovered(
    override val urlId: UrlId,
    val url: String,
    override val occurredAt: Instant
) : DomainEvent
```

### Ports (интерфейсы)

```kotlin
// Выходной порт — реализация будет в adapter/out
interface UrlRepository {
    suspend fun findById(id: UrlId): MonitoredUrl?
    suspend fun findAllActive(): List<MonitoredUrl>
    suspend fun findByOwner(owner: UserId): List<MonitoredUrl>
    suspend fun save(url: MonitoredUrl): MonitoredUrl
    suspend fun delete(id: UrlId)
}

interface HealthChecker {
    suspend fun check(url: String, timeoutMs: Long = 10_000): CheckResult
}

interface EventPublisher {
    suspend fun publish(event: DomainEvent)
}
```

---

## Ключевые корутинные паттерны

### 1. Параллельные проверки с ограничением concurrency

```kotlin
// domain/service/CheckOrchestrator.kt
class CheckOrchestrator(
    private val urlRepo: UrlRepository,
    private val checker: HealthChecker,
    private val resultRepo: CheckResultRepository,
    private val publisher: EventPublisher
) {
    suspend fun runAllChecks() = coroutineScope {
        val semaphore = Semaphore(20)  // макс. 20 одновременных проверок

        urlRepo.findAllActive().map { url ->
            async {
                semaphore.withPermit {
                    checkSingle(url)
                }
            }
        }.awaitAll()
    }

    private suspend fun checkSingle(url: MonitoredUrl) {
        val result = try {
            checker.check(url.url)
        } catch (e: Exception) {
            CheckResult(null, 0, e.message, Instant.now())
        }

        resultRepo.save(url.id, result)

        val (updated, event) = url.applyCheck(result)
        urlRepo.save(updated)
        event?.let { publisher.publish(it) }
    }
}
```

### 2. CoroutineVerticle с периодическим таймером

```kotlin
// adapter/in/scheduler/CheckSchedulerVerticle.kt
class CheckSchedulerVerticle(
    private val orchestrator: CheckOrchestrator
) : CoroutineVerticle() {

    override suspend fun start() {
        val intervalMs = config.getLong("check.intervalMs", 60_000)

        // Vert.x periodic timer + корутины
        vertx.setPeriodic(intervalMs) {
            launch {
                try {
                    orchestrator.runAllChecks()
                    logger.info { "Check cycle completed" }
                } catch (e: Exception) {
                    logger.error(e) { "Check cycle failed" }
                }
            }
        }

        logger.info { "Scheduler started, interval=${intervalMs}ms" }
    }
}
```

### 3. Health Checker на Vert.x WebClient

```kotlin
// adapter/out/http/VertxHealthChecker.kt
class VertxHealthChecker(private val client: WebClient) : HealthChecker {

    override suspend fun check(url: String, timeoutMs: Long): CheckResult {
        val start = System.nanoTime()
        return try {
            val response = client.getAbs(url)
                .timeout(timeoutMs)
                .send()
                .coAwait()    // suspend-обёртка Vert.x Future → coroutine

            val latency = (System.nanoTime() - start) / 1_000_000
            CheckResult(response.statusCode(), latency, null, Instant.now())
        } catch (e: Exception) {
            val latency = (System.nanoTime() - start) / 1_000_000
            CheckResult(null, latency, e.message, Instant.now())
        }
    }
}
```

### 4. Event Bus с kotlinx.serialization

```kotlin
// adapter/out/eventbus/EventBusPublisher.kt
class EventBusPublisher(private val vertx: Vertx) : EventPublisher {

    override suspend fun publish(event: DomainEvent) {
        val address = when (event) {
            is UrlWentDown -> "events.url.down"
            is UrlRecovered -> "events.url.recovered"
        }
        val json = Json.encodeToString(DomainEvent.serializer(), event)
        vertx.eventBus().publish(address, json)
    }
}

// В notification-service: слушатель
class EventBusListener(
    private val sender: NotificationSender
) : CoroutineVerticle() {

    override suspend fun start() {
        vertx.eventBus().consumer<String>("events.url.down") { message ->
            launch {
                val event = Json.decodeFromString<UrlWentDown>(message.body())
                sender.send("🔴 ${event.url} is DOWN since ${event.occurredAt}")
            }
        }

        vertx.eventBus().consumer<String>("events.url.recovered") { message ->
            launch {
                val event = Json.decodeFromString<UrlRecovered>(message.body())
                sender.send("🟢 ${event.url} recovered at ${event.occurredAt}")
            }
        }
    }
}
```

---

## REST API

```
POST   /api/auth/login          # → JWT токен
POST   /api/auth/register       # → создать пользователя

GET    /api/urls                 # → список URL текущего пользователя
POST   /api/urls                 # → добавить URL на мониторинг
GET    /api/urls/{id}            # → детали + текущий статус
DELETE /api/urls/{id}            # → убрать из мониторинга
GET    /api/urls/{id}/history    # → последние N проверок (пагинация)

GET    /api/dashboard            # → сводка: сколько UP/DOWN/DEGRADED
```

---

## Этапы разработки (3–4 недели)

### Неделя 1 — Фундамент

- [v] Инициализировать Gradle multi-module проект (`domain`, `monitor-service`, `notification-service`)
- [v] Domain-слой: модели, value objects, sealed events, интерфейсы портов
- [v] `MonitoringService` — use case для CRUD URL (unit-тесты без БД)
- [v] `CheckOrchestrator` — use case проверки (unit-тесты с мок-`HealthChecker`)
- [v] Docker Compose: PostgreSQL
- [v] Flyway-миграции для таблиц `users`, `monitored_urls`, `check_results`
- [v] `ExposedUrlRepository` + `ExposedCheckResultRepository` — реализация портов

### Неделя 2 — HTTP-слой и планировщик

- [v] API Verticle: маршруты, JWT-авторизация, DTO-маппинг
  - [v] `UserRepository` port + `ExposedUserRepository` (findByEmail, save, хэш пароля)
  - [v] DTO-классы: `LoginRequest`, `RegisterRequest`, `AuthResponse`, `UrlRequest`, `UrlResponse`, `CheckResultResponse` (`@Serializable`)
  - [v] Auth endpoints: `POST /api/auth/register` + `POST /api/auth/login` (генерация JWT через `vertx-auth-jwt`)
  - [v] JWT middleware: `router.route("/api/*").handler(jwtHandler)`, извлечение `UserId` в `ctx`
  - [v] URL CRUD: `GET /api/urls`, `POST /api/urls`, `GET /api/urls/{id}`, `DELETE /api/urls/{id}` → `MonitoringService`
  - [v] History: `GET /api/urls/{id}/history` с `limit`/`offset` из query params
  - [v] DI: передать `MonitoringService`, `UserRepository`, `JWTAuth` в `ApiVerticle` через конструктор; задеплоить из `Main.kt`
- [v] `VertxHealthChecker` — реализация на WebClient с корутинами
- [v] `CheckSchedulerVerticle` — периодический запуск проверок
- [v] OpenAPI-спецификация (ручная или через `vertx-web-openapi`)
- [ ] Интеграционные тесты: Testcontainers + PostgreSQL, тест полного цикла проверки
- [ ] Dockerfile для `monitor-service` (multi-stage build)

### Неделя 3 — Notification-сервис и Event Bus

- [ ] Настроить Vert.x clustered Event Bus (Hazelcast или Infinispan)
- [ ] `EventBusPublisher` в monitor-service
- [ ] `EventBusListener` в notification-service
- [ ] `TelegramSender` (или `LogSender` как заглушка)
- [ ] Docker Compose: оба сервиса в одной сети
- [ ] E2E тест: добавить URL → дождаться проверки → получить событие

### Неделя 4 — Полировка и продвинутые фичи

- [ ] Dashboard endpoint с агрегацией по статусам
- [ ] Пагинация истории проверок (cursor-based)
- [ ] Graceful shutdown: `CoroutineScope` привязан к жизни Verticle
- [ ] Health/readiness endpoints для самих сервисов
- [ ] Метрики: количество проверок, средняя латентность (Micrometer)
- [ ] README с инструкцией запуска, архитектурной диаграммой, примерами curl
- [ ] Рефакторинг по итогам ревью всего кода

---

## Стек технологий

| Слой | Технология |
|------|-----------|
| Фреймворк | Vert.x 5 + `vertx-lang-kotlin-coroutines` |
| HTTP-клиент (для проб) | Vert.x WebClient |
| Сериализация | kotlinx.serialization |
| БД | PostgreSQL + Exposed (Kotlin SQL DSL) |
| Миграции | Flyway |
| Аутентификация | JWT (`vertx-auth-jwt`) |
| Event Bus | Vert.x Clustered Event Bus |
| Логирование | kotlin-logging + Logback |
| Конфигурация | Vert.x Config (HOCON) |
| Тестирование | Kotest + MockK + Testcontainers |
| Сборка | Gradle Kotlin DSL (multi-module) |
| Контейнеризация | Docker + Docker Compose |

---

## Что ты закрепишь

**Корутины:**
- `coroutineScope` + `async` для параллельных проверок с `Semaphore`
- `CoroutineVerticle` как мост между event-loop Vert.x и suspend-миром
- `.coAwait()` для оборачивания Vert.x `Future` в корутины
- Structured concurrency: привязка scope к lifecycle Verticle
- Обработка ошибок: `supervisorScope`, try/catch в `launch`

**Clean Architecture:**
- Domain-слой без единого `import io.vertx` — чистая бизнес-логика
- Порты (interfaces) в domain, адаптеры (implementations) снаружи
- Направление зависимостей: adapters → domain, никогда наоборот
- Use Cases как точки входа в бизнес-логику

**DDD-lite:**
- Value Objects (`UrlId`, `UserId`, `CheckResult`) — типобезопасность вместо примитивов
- Domain Events (`UrlWentDown`, `UrlRecovered`) — развязка между контекстами
- Bounded Contexts: Monitoring и Notification — разные сервисы, разные модели
- Entity с поведением: `MonitoredUrl.applyCheck()` — логика внутри модели

---

## Бонусные идеи (если останется время)

- **Redis-кэш** для dashboard-агрегации (обновлять при каждом цикле проверок)
- **SSE endpoint** (`/api/events/stream`) — real-time поток событий в браузер
- **Retry с exponential backoff** — при временных сбоях проверки
- **Rate limiting** — ограничить количество URL на пользователя
- **Мультирегиональные проверки** — несколько Checker Verticle с разными IP
