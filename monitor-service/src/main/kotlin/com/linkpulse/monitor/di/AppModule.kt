package com.linkpulse.monitor.di

import com.linkpulse.domain.port.CheckResultRepository
import com.linkpulse.domain.port.EventPublisher
import com.linkpulse.domain.port.HealthChecker
import com.linkpulse.domain.port.PasswordHasher
import com.linkpulse.domain.port.UrlRepository
import com.linkpulse.domain.port.UserRepository
import com.linkpulse.domain.port.auth.UserLoginer
import com.linkpulse.domain.port.auth.UserRegistrar
import com.linkpulse.domain.service.CheckOrchestrator
import com.linkpulse.domain.service.MonitoringService
import com.linkpulse.domain.service.UserService
import com.linkpulse.monitor.adapter.output.BCryptPasswordHasher
import com.linkpulse.monitor.adapter.output.eventbus.VertxEventPublisher
import com.linkpulse.monitor.adapter.output.http.VertxHealthChecker
import com.linkpulse.monitor.adapter.output.persistence.ExposedCheckResultRepository
import com.linkpulse.monitor.adapter.output.persistence.ExposedUrlRepository
import com.linkpulse.monitor.adapter.output.persistence.ExposedUserRepository
import org.koin.dsl.module

val appModule = module {

    // ── Domain ──
    single<UserRepository> { ExposedUserRepository() }
    single<UrlRepository> { ExposedUrlRepository() }
    single<CheckResultRepository> { ExposedCheckResultRepository() }
    single<PasswordHasher> { BCryptPasswordHasher() }

    // ── Use cases ──
    single { UserService(get(), get()) }
    single<UserRegistrar> { get<UserService>() }
    single<UserLoginer> { get<UserService>() }
    single { MonitoringService(get(), get()) }

    // ── HTTP Client (предоставляется извне в Main.kt) ──
    single<HealthChecker> { VertxHealthChecker(get(), get()) }

    // ── Event Publisher (предоставляется извне в Main.kt) ──
    single<EventPublisher> { VertxEventPublisher(get()) }

    // ── Check Orchestrator ──
    single { CheckOrchestrator(get(), get(), get(), get()) }
}