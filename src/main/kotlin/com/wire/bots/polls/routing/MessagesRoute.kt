package com.wire.bots.polls.routing

import com.wire.bots.polls.dto.roman.Message
import com.wire.bots.polls.services.AuthService
import com.wire.bots.polls.services.MessagesHandlingService
import com.wire.bots.polls.services.UserCommunicationService
import com.wire.bots.polls.setup.logging.USER_ID
import com.wire.bots.polls.utils.mdc
import com.wire.integrations.jvm.WireAppSdk
import com.wire.integrations.jvm.WireEventsHandler
import com.wire.integrations.jvm.model.ConversationData
import com.wire.integrations.jvm.model.ConversationMember
import com.wire.integrations.jvm.model.WireMessage
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.post
import org.kodein.di.instance
import org.kodein.di.ktor.closestDI
import java.util.UUID

/**
 * Messages API.
 */
fun Routing.messages() {
    val k = closestDI()
    val handler by k.instance<MessagesHandlingService>()
    val communicate by k.instance<UserCommunicationService>()
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
                // includes user id to current MDC
                mdc(USER_ID) { it.userId }

                handler.handle(it)
                routingLogger.debug { "Responding OK" }
                call.respond(HttpStatusCode.OK)
            }
        } else {
            routingLogger.warn { "Token is invalid." }
            call.respond(HttpStatusCode.Unauthorized, "Please provide Authorization header.")
        }
    }
    val wireAppSdk = WireAppSdk(
        applicationId = UUID.randomUUID(),
        apiToken = "myApiToken",
        apiHost = "https://nginz-https.chala.wire.link",
        cryptographyStoragePassword = "myDummyPassword",
        object : WireEventsHandler() {
            override suspend fun onNewMessageSuspending(wireMessage: WireMessage.Text) {
                val message = WireMessage.Text.create(
                    conversationId = wireMessage.conversationId,
                    text = "${wireMessage.text} -- Sent from the SDK"
                )

                manager.sendMessageSuspending(
                    conversationId = wireMessage.conversationId,
                    message = message
                )
            }

            override fun onConversationJoin(
                conversation: ConversationData,
                members: List<ConversationMember>
            ) {
                val message = WireMessage.Text.create(
                    conversationId = conversation.id,
                    text = communicate.sayHello().type
                )

                manager.sendMessage(
                    conversationId = conversation.id,
                    message = message
                )
            }
        }
    )
    wireAppSdk.startListening()
}
