package com.wire.bots.polls.setup.errors

import com.wire.bots.polls.utils.countException
import com.wire.bots.polls.utils.createLogger
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.response.*
import io.micrometer.prometheus.PrometheusMeterRegistry
import org.kodein.di.instance
import org.kodein.di.ktor.closestDI

private val logger = createLogger("ExceptionHandler")

/**
 * Registers exception handling.
 */
fun Application.registerExceptionHandlers() {
    val registry by closestDI().instance<PrometheusMeterRegistry>()

    install(StatusPages) {
        exception<Exception> { cause ->
            logger.error(cause) { "Exception occurred in the application: ${cause.message}" }
            call.errorResponse(HttpStatusCode.InternalServerError, cause.message)
            registry.countException(cause)
        }

        exception<RomanUnavailableException> { cause ->
            logger.error(cause) {
                "Error in communication with Roman. Status: ${cause.status}, body: ${cause.body}."
            }
            call.errorResponse(HttpStatusCode.ServiceUnavailable, cause.message)
            registry.countException(cause)
        }
    }
}

suspend inline fun ApplicationCall.errorResponse(
    statusCode: HttpStatusCode,
    message: String?
) {
    respond(status = statusCode, message = mapOf("message" to (message ?: "No details specified")))
}
