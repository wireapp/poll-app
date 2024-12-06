package com.wire.bots.polls.setup

import io.ktor.application.*
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
