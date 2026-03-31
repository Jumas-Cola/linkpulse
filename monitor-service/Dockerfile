# ── Этап 1: сборка ──
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY gradlew gradle.properties settings.gradle.kts build.gradle.kts ./
COPY gradle/ gradle/
COPY domain/ domain/
COPY monitor-service/ monitor-service/
RUN chmod +x gradlew && ./gradlew :monitor-service:fatJar --no-daemon

# ── Этап 2: запуск ──
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/monitor-service/build/libs/*-fat.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
