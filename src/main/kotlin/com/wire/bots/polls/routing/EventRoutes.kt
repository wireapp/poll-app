package com.wire.bots.polls.routing

import com.wire.bots.polls.services.MessagesHandlingService
import com.wire.integrations.jvm.WireAppSdk
import com.wire.integrations.jvm.WireEventsHandler
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
        object : WireEventsHandler() {
            override fun onConversationJoin(
                conversation: ConversationData,
                members: List<ConversationMember>
            ) {
                handler.handleConversationJoin(manager, conversation.id)
            }

            override suspend fun onNewMessageSuspending(wireMessage: WireMessage.Text) {
                handler.handleText(manager, wireMessage)
            }

            override suspend fun onNewButtonActionSuspending(
                wireMessage: WireMessage.ButtonAction
            ) {
                handler.handleButtonAction(manager, wireMessage)
            }
        }
    )
    wireAppSdk.startListening()
}
