package com.wire.apps.polls.routing

import com.wire.apps.polls.dto.PollActionMapper
import com.wire.apps.polls.dto.UsersInput.Companion.fromWire
import com.wire.apps.polls.setup.conf.SDKConfiguration
import com.wire.apps.polls.services.MessagesHandlingService
import com.wire.sdk.WireAppSdk
import com.wire.sdk.WireEventsHandlerSuspending
import com.wire.sdk.model.ConversationData
import com.wire.sdk.model.ConversationMember
import com.wire.sdk.model.WireMessage
import io.ktor.server.routing.Routing
import org.kodein.di.instance
import org.kodein.di.ktor.closestDI
import kotlin.getValue

/**
 * Events API.
 */
fun Routing.events() {
    val di = closestDI()
    val handler by di.instance<MessagesHandlingService>()
    val sdkConfig by di.instance<SDKConfiguration>()
    val pollActionMapper by di.instance<PollActionMapper>()

    val wireAppSdk = WireAppSdk(
        applicationId = sdkConfig.appId,
        apiToken = sdkConfig.appToken,
        apiHost = sdkConfig.apiHostUrl,
        cryptographyStoragePassword = sdkConfig.cryptoPassword,
        wireEventsHandler = object : WireEventsHandlerSuspending() {
            override suspend fun onAppAddedToConversation(
                conversation: ConversationData,
                members: List<ConversationMember>
            ) {
                handler.handleConversationJoin(manager, conversation.id)
            }

            override suspend fun onTextMessageReceived(wireMessage: WireMessage.Text) {
                val usersInput = fromWire(wireMessage)

                handler.handleUserCommand(manager, usersInput)
            }

            override suspend fun onButtonClicked(wireMessage: WireMessage.ButtonAction) {
                val pollAction = pollActionMapper.fromButtonAction(wireMessage) ?: return

                handler.handlePollAction(
                    manager = manager,
                    pollAction = pollAction,
                    conversationId = wireMessage.conversationId
                )
            }
        }
    )
    wireAppSdk.startListening()
}
