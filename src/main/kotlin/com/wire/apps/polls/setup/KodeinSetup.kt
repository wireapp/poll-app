package com.wire.apps.polls.setup

import org.kodein.di.ktor.di
import io.ktor.application.Application

/**
 * Inits and sets up DI container.
 */
fun Application.setupKodein() {
    di {
        bindConfiguration()
        configureContainer()
    }
}
