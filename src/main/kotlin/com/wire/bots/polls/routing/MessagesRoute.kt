package com.wire.bots.polls.routing

import com.wire.bots.polls.services.MessagesHandlingService
import com.wire.bots.polls.services.UserCommunicationService
import com.wire.integrations.jvm.WireAppSdk
import com.wire.integrations.jvm.WireEventsHandler
import com.wire.integrations.jvm.model.ConversationData
import com.wire.integrations.jvm.model.ConversationMember
import com.wire.integrations.jvm.model.WireMessage
import io.ktor.routing.Routing
import kotlinx.coroutines.runBlocking
import org.kodein.di.instance
import org.kodein.di.ktor.closestDI
import java.util.UUID

/**
 * Messages API.
 */
fun Routing.messages() {
    val k = closestDI()
    val handler by k.instance<MessagesHandlingService>()
    val userCommunicationService by k.instance<UserCommunicationService>()

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
                handler.handleText(manager, wireMessage)
            }

            override fun onConversationJoin(
                conversation: ConversationData,
                members: List<ConversationMember>
            ) {
                runBlocking {
                    userCommunicationService.sayHello(manager, conversation.id)
                }
            }
        }
    )
    wireAppSdk.startListening()
}
