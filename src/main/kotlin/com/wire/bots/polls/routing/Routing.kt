package com.wire.bots.polls.routing

import io.ktor.routing.Routing

/**
 * Register routes to the KTor.
 */
fun Routing.registerRoutes() {
    serviceRoutes()
    events()
}
