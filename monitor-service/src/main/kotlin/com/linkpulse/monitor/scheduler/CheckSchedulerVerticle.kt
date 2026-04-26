package com.linkpulse.monitor.scheduler

import com.linkpulse.domain.service.CheckOrchestrator
import io.github.oshai.kotlinlogging.KotlinLogging
import io.vertx.kotlin.coroutines.CoroutineVerticle
import kotlinx.coroutines.delay
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named

private val logger = KotlinLogging.logger {}

class CheckSchedulerVerticle : CoroutineVerticle(), KoinComponent {

    private val checkOrchestrator: CheckOrchestrator by inject()

    private val intervalMs: Long by inject(named("checker.intervalMs"))

    override suspend fun start() {
        logger.info { "CheckScheduler started with interval ${intervalMs}ms" }
        
        while (true) {
            try {
                logger.info { "Started checks" }

                checkOrchestrator.runAllChecks()

                logger.info { "Finished checks" }
            } catch (e: Exception) {
                logger.error(e) { "Error running checks" }
            }
            delay(intervalMs)
        }
    }
}