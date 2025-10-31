package com.wire.apps.polls.setup

import io.ktor.server.application.Application
import org.kodein.di.ktor.di

/**
 * Inits and sets up DI container.
 */
fun Application.setupKodein() {
    di {
        bindConfiguration()
        configureContainer()
    }
}
