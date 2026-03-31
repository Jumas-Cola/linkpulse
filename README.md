# LinkPulse — URL Monitoring Service

Два Vert.x-сервиса для мониторинга доступности URL с уведомлениями при даунтайме.

## Требования

- **JDK 21** — [Eclipse Temurin](https://adoptium.net)
- **Gradle 8.12+** — [gradle.org/install](https://gradle.org/install/)
- **Docker** и **Docker Compose** — для PostgreSQL

## Быстрый старт

```bash
# 1. Инициализировать Gradle Wrapper (один раз)
gradle wrapper --gradle-version 8.12

# 2. Поднять PostgreSQL
docker compose up -d postgres

# 3. Собрать и проверить
./gradlew build

# 4. Запустить monitor-service
./gradlew :monitor-service:run

# 5. Проверить
curl http://localhost:8080/health
```

## Структура

```
linkpulse/
├── domain/                  # Чистая бизнес-логика (без фреймворков)
├── monitor-service/         # REST API + планировщик проверок
├── notification-service/    # Слушатель Event Bus → уведомления
└── docker-compose.yml       # PostgreSQL
```

## Тестирование

```bash
# Unit-тесты domain
./gradlew :domain:test

# Все тесты
./gradlew test
```
