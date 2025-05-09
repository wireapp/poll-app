package com.wire.apps.polls.services

import com.wire.apps.polls.dto.UsersInput
import com.wire.integrations.jvm.model.QualifiedId
import com.wire.integrations.jvm.model.WireMessage
import com.wire.integrations.jvm.service.WireApplicationManager
import io.ktor.features.BadRequestException
import kotlinx.coroutines.runBlocking
import mu.KLogging

/**
 * Connect conversation-based events to app functionality
 */
class MessagesHandlingService(
    private val pollService: PollService,
    private val userCommunicationService: UserCommunicationService
) {
    private companion object : KLogging()

    /**
     * Welcomes the user by listing available commands.
     */
    fun handleConversationJoin(
        manager: WireApplicationManager,
        conversationId: QualifiedId
    ) {
        runBlocking { userCommunicationService.sayHello(manager, conversationId) }
    }

    /**
     * Records the user's vote based on the selected button.
     */
    suspend fun handleButtonAction(
        manager: WireApplicationManager,
        message: WireMessage.ButtonAction
    ) {
        pollService.pollAction(
            manager,
            message,
            message.conversationId
        )
    }

    /**
     * Makes app interactive by executing user-issued commands.
     */
    suspend fun handleText(
        manager: WireApplicationManager,
        message: WireMessage.Text
    ) {
        with(message) {
            if (sender == null) {
                throw BadRequestException("UserId must be set for text messages.")
            } else if (text != null) {
                val trimmed = (text as String).trim()
                when {
                    // stats request
                    trimmed.startsWith("/poll stats") -> {
                        pollService.sendStatsForLatest(manager, conversationId)
                    }
                    // send version when asked
                    trimmed.startsWith("/poll version") -> {
                        userCommunicationService.sendVersion(manager, conversationId)
                    }
                    // send version when asked
                    trimmed.startsWith("/poll help") -> {
                        userCommunicationService.sendHelp(manager, conversationId)
                    }
                    // poll request
                    trimmed.startsWith("/poll") ->
                        pollService.createPoll(
                            manager,
                            conversationId,
                            UsersInput(
                                userId = sender,
                                input = trimmed,
                                mentions = mentions
                            )
                        )
                    // Easter egg, good app is good
                    trimmed == "good app" -> {
                        userCommunicationService.goodApp(manager, conversationId)
                    }
                    else -> logger.debug("Ignoring the message, unrecognized command.")
                }
            } else {
                logger.debug("Ignoring message as it does not have correct fields set.")
            }
        }
    }
}
