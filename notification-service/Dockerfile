FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY gradlew gradle.properties settings.gradle.kts build.gradle.kts ./
COPY gradle/ gradle/
COPY domain/ domain/
COPY notification-service/ notification-service/
RUN chmod +x gradlew && ./gradlew :notification-service:fatJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/notification-service/build/libs/*-fat.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
