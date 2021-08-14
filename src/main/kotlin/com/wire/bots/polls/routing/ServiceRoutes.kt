package com.wire.bots.polls.routing

import com.wire.bots.polls.dao.DatabaseSetup
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.response.respondTextWriter
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.micrometer.prometheus.PrometheusMeterRegistry
import org.kodein.di.instance
import org.kodein.di.ktor.closestDI

/**
 * Registers prometheus data.
 */
fun Routing.serviceRoutes() {
    val k = closestDI()
    val version by k.instance<String>("version")
    val registry by k.instance<PrometheusMeterRegistry>()

    /**
     * Information about service.
     */
    get("/") {
        call.respond("Server running version: \"$version\".")
    }

    /**
     * Send data about version.
     */
    get("/version") {
        call.respond(mapOf("version" to version))
    }

    /**
     * Responds only 200 for ingres.
     */
    get("/status") {
        call.respond(HttpStatusCode.OK)
    }

    /**
     * More complex API for indication of all resources.
     */
    get("/status/health") {
        if (DatabaseSetup.isConnected()) {
            call.respond(mapOf("health" to "healthy"))
        } else {
            call.respond(HttpStatusCode.ServiceUnavailable, "DB connection is not working")
        }
    }

    /**
     * Prometheus endpoint.
     */
    get("/metrics") {
        call.respondTextWriter(status = HttpStatusCode.OK) {
            @Suppress("BlockingMethodInNonBlockingContext") // sadly this is synchronous API
            registry.scrape(this)
        }
    }
}
