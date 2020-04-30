package com.wire.bots.polls.routing

import com.wire.bots.polls.dto.roman.Message
import com.wire.bots.polls.services.AuthService
import com.wire.bots.polls.services.MessagesHandlingService
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.post
import org.kodein.di.LazyKodein
import org.kodein.di.generic.instance

/**
 * Messages API.
 */
fun Routing.messages(k: LazyKodein) {
    val handler by k.instance<MessagesHandlingService>()
    val authService by k.instance<AuthService>()

    /**
     * API for receiving messages from Roman.
     */
    post("/messages") {
        routingLogger.debug { "POST /messages" }
        // verify whether request contain correct auth header
        if (authService.isTokenValid { call.request.headers }) {
            routingLogger.debug { "Token is valid." }
            // bot responds either with 200 or with 400
            runCatching {
                routingLogger.debug { "Parsing an message." }
                call.receive<Message>()
            }.onFailure {
                routingLogger.error(it) { "Exception occurred during the request handling!" }
                call.respond(HttpStatusCode.BadRequest, "Bot did not understand the message.")
            }.onSuccess {
                routingLogger.debug { "Message parsed." }
                handler.handle(it)
                routingLogger.debug { "Responding OK" }
                call.respond(HttpStatusCode.OK)
            }
        } else {
            routingLogger.warn { "Token is invalid." }
            call.respond(HttpStatusCode.Unauthorized, "Please provide Authorization header.")
        }
    }
}
