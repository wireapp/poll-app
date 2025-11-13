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
import org.slf4j.LoggerFactory
import kotlin.getValue

/**
 * Events API.
 */
fun Routing.events() {
    val di = closestDI()
    val handler by di.instance<MessagesHandlingService>()
    val sdkConfig by di.instance<SDKConfiguration>()
    val pollActionMapper by di.instance<PollActionMapper>()
    val logger = LoggerFactory.getLogger(this::class.java)

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
                logger.info(
                    "Event received. Event: AppAddedToConversation, " +
                        "conversationId: ${conversation.id}"
                )
                handler.handleAppAddedToConversation(manager, conversation.id)
                logger.info("App added to conversation. conversationId: ${conversation.id}")
            }

            override suspend fun onTextMessageReceived(wireMessage: WireMessage.Text) {
                logger.info(
                    "Event received. Event: TextMessageReceived, " +
                        "conversationId: ${wireMessage.conversationId}, " +
                        "messageId: ${wireMessage.id}, " +
                        "senderId: ${wireMessage.sender.id}"
                )
                val usersInput = fromWire(wireMessage)
                // TODO :: It is better to validate the command here
                //  and not go to the handler if the command is invalid
                handler.handleUserCommand(manager, usersInput)
                logger.info(
                    "Text message is processed. conversationId: ${wireMessage.conversationId}"
                )
            }

            override suspend fun onButtonClicked(wireMessage: WireMessage.ButtonAction) {
                logger.info(
                    "Event received. Event: ButtonClicked, " +
                        "conversationId: ${wireMessage.conversationId}, " +
                        "messageId: ${wireMessage.id}, " +
                        "senderId: ${wireMessage.sender.id}"
                )
                val pollAction = pollActionMapper.fromButtonAction(wireMessage) ?: return

                handler.handlePollAction(
                    manager = manager,
                    pollAction = pollAction,
                    conversationId = wireMessage.conversationId
                )
                logger.info(
                    "Button click on poll is processed. conversationId: ${wireMessage.conversationId}"
                )
            }
        }
    )
    wireAppSdk.startListening()
}
