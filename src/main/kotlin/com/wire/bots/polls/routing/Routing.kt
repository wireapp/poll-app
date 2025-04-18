package com.wire.bots.polls.routing

import com.wire.bots.polls.utils.createLogger
import io.ktor.routing.Routing

internal val routingLogger by lazy { createLogger("RoutingLogger") }

/**
 * Register routes to the KTor.
 */
fun Routing.registerRoutes() {
    serviceRoutes()
    messages()
}
