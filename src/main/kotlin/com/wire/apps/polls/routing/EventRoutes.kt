package com.wire.apps.polls.routing

import com.wire.apps.polls.services.ButtonActionIdentifier
import com.wire.apps.polls.dto.UsersInput.Companion.fromWire
import com.wire.apps.polls.setup.conf.SDKConfiguration
import com.wire.apps.polls.services.MessagesHandlingService
import com.wire.integrations.jvm.WireAppSdk
import com.wire.integrations.jvm.WireEventsHandlerSuspending
import com.wire.integrations.jvm.model.ConversationData
import com.wire.integrations.jvm.model.ConversationMember
import com.wire.integrations.jvm.model.WireMessage
import io.ktor.routing.Routing
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
    val buttonIdentifier by di.instance<ButtonActionIdentifier>()

    val wireAppSdk = WireAppSdk(
        applicationId = sdkConfig.appId,
        apiToken = sdkConfig.appToken,
        apiHost = sdkConfig.apiHostUrl,
        cryptographyStoragePassword = sdkConfig.cryptoPassword,
        wireEventsHandler = object : WireEventsHandlerSuspending() {
            override suspend fun onConversationJoin(
                conversation: ConversationData,
                members: List<ConversationMember>
            ) {
                handler.handleConversationJoin(manager, conversation.id)
            }

            override suspend fun onMessage(wireMessage: WireMessage.Text) {
                val usersInput = fromWire(wireMessage)

                handler.handleText(manager, usersInput)
            }

            override suspend fun onButtonAction(wireMessage: WireMessage.ButtonAction) {
                val buttonPressed = buttonIdentifier.fromWire(wireMessage) ?: return

                handler.handleButtonPressed(
                    manager = manager,
                    buttonPressed = buttonPressed,
                    conversationId = wireMessage.conversationId
                )
            }
        }
    )
    wireAppSdk.startListening()
}
