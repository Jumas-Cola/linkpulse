plugins {
    kotlin("plugin.serialization")
    application
}

application {
    mainClass.set("com.linkpulse.monitor.MainKt")
}

dependencies {
    // ── Domain ──
    implementation(project(":domain"))

    // ── Vert.x Core ──
    implementation("io.vertx:vertx-core:${property("vertxVersion")}")
    implementation("io.vertx:vertx-web:${property("vertxVersion")}")
    implementation("io.vertx:vertx-web-client:${property("vertxVersion")}")
    implementation("io.vertx:vertx-config:${property("vertxVersion")}")
    implementation("io.vertx:vertx-config-hocon:${property("vertxVersion")}")

    // ── Vert.x Kotlin + Корутины ──
    implementation("io.vertx:vertx-lang-kotlin:${property("vertxVersion")}")
    implementation("io.vertx:vertx-lang-kotlin-coroutines:${property("vertxVersion")}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${property("coroutinesVersion")}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:${property("coroutinesVersion")}")

    // ── Vert.x Auth (JWT) ──
    implementation("io.vertx:vertx-auth-jwt:${property("vertxVersion")}")

    // ── Vert.x Кластеризация (Event Bus между сервисами) ──
    implementation("io.vertx:vertx-hazelcast:${property("vertxVersion")}")

    // ── Сериализация ──
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${property("serializationVersion")}")

    // ── DI ──
    implementation("io.insert-koin:koin-core:${property("koinVersion")}")

    // ── Хэширование паролей ──
    implementation("at.favre.lib:bcrypt:${property("bcryptVersion")}")

    // ── База данных ──
    implementation("org.jetbrains.exposed:exposed-core:${property("exposedVersion")}")
    implementation("org.jetbrains.exposed:exposed-jdbc:${property("exposedVersion")}")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:${property("exposedVersion")}")
    implementation("org.postgresql:postgresql:${property("postgresDriverVersion")}")
    implementation("com.zaxxer:HikariCP:${property("hikariVersion")}")

    // ── Миграции ──
    implementation("org.flywaydb:flyway-core:${property("flywayVersion")}")
    implementation("org.flywaydb:flyway-database-postgresql:${property("flywayVersion")}")

    // ── Логирование ──
    implementation("io.github.oshai:kotlin-logging-jvm:${property("kotlinLoggingVersion")}")
    implementation("ch.qos.logback:logback-classic:${property("logbackVersion")}")

    // ── Тестирование ──
    testImplementation(kotlin("test"))
    testImplementation("io.vertx:vertx-junit5:${property("vertxVersion")}")
    testImplementation("io.kotest:kotest-runner-junit5:${property("kotestVersion")}")
    testImplementation("io.kotest:kotest-assertions-core:${property("kotestVersion")}")
    testImplementation("io.mockk:mockk:${property("mockkVersion")}")
    testImplementation("org.testcontainers:testcontainers:${property("testcontainersVersion")}")
    testImplementation("org.testcontainers:postgresql:${property("testcontainersVersion")}")
    testImplementation("org.testcontainers:junit-jupiter:${property("testcontainersVersion")}")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${property("coroutinesVersion")}")
}

tasks.withType<Test> {
    environment("DOCKER_API_VERSION", "1.41")
}

tasks.register<Jar>("fatJar") {
    archiveClassifier.set("fat")
    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    with(tasks.jar.get())
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
