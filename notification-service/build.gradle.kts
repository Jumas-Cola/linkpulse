plugins {
    kotlin("plugin.serialization")
    application
}

application {
    mainClass.set("com.linkpulse.notification.MainKt")
}

dependencies {
    // ── Domain ──
    implementation(project(":domain"))

    // ── Vert.x Core ──
    implementation("io.vertx:vertx-core:${property("vertxVersion")}")
    implementation("io.vertx:vertx-web-client:${property("vertxVersion")}")

    // ── Vert.x Kotlin + Корутины ──
    implementation("io.vertx:vertx-lang-kotlin:${property("vertxVersion")}")
    implementation("io.vertx:vertx-lang-kotlin-coroutines:${property("vertxVersion")}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${property("coroutinesVersion")}")

    // ── Vert.x Кластеризация ──
    implementation("io.vertx:vertx-hazelcast:${property("vertxVersion")}")

    // ── Сериализация ──
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${property("serializationVersion")}")

    // ── Логирование ──
    implementation("io.github.oshai:kotlin-logging-jvm:${property("kotlinLoggingVersion")}")
    implementation("ch.qos.logback:logback-classic:${property("logbackVersion")}")

    // ── Тестирование ──
    testImplementation(kotlin("test"))
    testImplementation("io.vertx:vertx-junit5:${property("vertxVersion")}")
    testImplementation("io.kotest:kotest-runner-junit5:${property("kotestVersion")}")
    testImplementation("io.kotest:kotest-assertions-core:${property("kotestVersion")}")
    testImplementation("io.mockk:mockk:${property("mockkVersion")}")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${property("coroutinesVersion")}")
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
