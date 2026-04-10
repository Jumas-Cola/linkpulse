package com.linkpulse.monitor.di

import com.linkpulse.domain.port.PasswordHasher
import com.linkpulse.domain.port.UrlRepository
import com.linkpulse.domain.port.UserRepository
import com.linkpulse.domain.port.auth.UserLoginer
import com.linkpulse.domain.port.auth.UserRegistrar
import com.linkpulse.domain.service.UserService
import com.linkpulse.monitor.adapter.output.BCryptPasswordHasher
import com.linkpulse.monitor.adapter.output.persistence.ExposedUrlRepository
import com.linkpulse.monitor.adapter.output.persistence.ExposedUserRepository
import org.koin.dsl.module

val appModule = module {

    // ── Адаптеры (реализации портов) ──
    single<UserRepository> { ExposedUserRepository() }
    single<UrlRepository> { ExposedUrlRepository() }
    single<PasswordHasher> { BCryptPasswordHasher() }

    // ── Use cases ──
    // UserService реализует оба интерфейса — один инстанс, два типа
    single { UserService(get(), get()) }
    single<UserRegistrar> { get<UserService>() }
    single<UserLoginer> { get<UserService>() }
}