package com.wire.apps.polls.routing

import com.wire.apps.polls.dto.PollAction.Companion.fromWire
import com.wire.apps.polls.dto.UsersInput.Companion.fromWire
import com.wire.apps.polls.services.MessagesHandlingService
import com.wire.integrations.jvm.WireAppSdk
import com.wire.integrations.jvm.WireEventsHandlerSuspending
import com.wire.integrations.jvm.model.ConversationData
import com.wire.integrations.jvm.model.ConversationMember
import com.wire.integrations.jvm.model.WireMessage
import io.ktor.routing.Routing
import org.kodein.di.instance
import org.kodein.di.ktor.closestDI
import java.util.UUID

/**
 * Events API.
 */
fun Routing.events() {
    val handler by closestDI().instance<MessagesHandlingService>()

    val wireAppSdk = WireAppSdk(
        applicationId = UUID.randomUUID(),
        apiToken = "myApiToken",
        apiHost = "https://nginz-https.chala.wire.link",
        cryptographyStoragePassword = "myDummyPassword",
        wireEventsHandler = object : WireEventsHandlerSuspending() {
            override suspend fun onConversationJoin(
                conversation: ConversationData,
                members: List<ConversationMember>
            ) {
                handler.handleConversationJoin(manager, conversation.id)
            }

            override suspend fun onMessage(message: WireMessage.Text) {
                val usersInput = fromWire(message)
                handler.handleText(manager, usersInput)
            }

            override suspend fun onButtonAction(message: WireMessage.ButtonAction) {
                val pollAction = fromWire(message)
                handler.handleButtonAction(
                    manager = manager,
                    pollAction = pollAction,
                    conversationId = message.conversationId
                )
            }
        }
    )
    wireAppSdk.startListening()
}
