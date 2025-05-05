package com.wire.bots.polls.routing

import com.wire.bots.polls.services.MessagesHandlingService
import com.wire.bots.polls.services.PollService
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
    val pollService by k.instance<PollService>()

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

            override suspend fun onNewButtonActionSuspending(
                wireMessage: WireMessage.ButtonAction
            ) {
                pollService.pollAction(
                    manager,
                    wireMessage,
                    wireMessage.conversationId
                )
            }
        }
    )
    wireAppSdk.startListening()
}
