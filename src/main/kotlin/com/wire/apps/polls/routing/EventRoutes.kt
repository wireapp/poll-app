package com.wire.apps.polls.routing

import com.wire.apps.polls.dto.common.mapToPollAction
import com.wire.apps.polls.dto.common.mapToUsersInput
import com.wire.apps.polls.services.MessagesHandlingService
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
    val k = closestDI()
    val handler by k.instance<MessagesHandlingService>()

    val wireAppSdk = WireAppSdk(
        applicationId = UUID.randomUUID(),
        apiToken = "myApiToken",
        apiHost = "https://nginz-https.chala.wire.link",
        cryptographyStoragePassword = "myDummyPassword",
        wireEventsHandler = object : WireEventsHandler() {
            override fun onConversationJoin(
                conversation: ConversationData,
                members: List<ConversationMember>
            ) {
                handler.handleConversationJoin(manager, conversation.id)
            }

            override suspend fun onNewMessageSuspending(wireMessage: WireMessage.Text) {
                val usersInput = mapToUsersInput(wireMessage)
                handler.handleText(manager, usersInput)
            }

            override suspend fun onNewButtonActionSuspending(
                wireMessage: WireMessage.ButtonAction
            ) {
                val pollAction = mapToPollAction(wireMessage)
                handler.handleButtonAction(
                    manager = manager,
                    pollAction = pollAction,
                    conversationId = wireMessage.conversationId
                )
            }
        }
    )
    wireAppSdk.startListening()
}
