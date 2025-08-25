package com.wire.apps.polls.services

import com.wire.apps.polls.dto.CompositeButtonAction
import com.wire.apps.polls.dto.CompositeButtonAction.PollAction
import com.wire.apps.polls.dto.CompositeButtonAction.ShowResultsAction
import com.wire.apps.polls.dto.UsersInput
import com.wire.integrations.jvm.model.QualifiedId
import com.wire.integrations.jvm.service.WireApplicationManager
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
    suspend fun handleConversationJoin(
        manager: WireApplicationManager,
        conversationId: QualifiedId
    ) {
        userCommunicationService.sayHello(manager, conversationId)
    }

    /**
     * Records the user's vote based on the selected button.
     */
    suspend fun handleButtonAction(
        manager: WireApplicationManager,
        compositeButtonAction: CompositeButtonAction,
        conversationId: QualifiedId
    ) {
        when (compositeButtonAction) {
            is PollAction -> {
                pollService.pollAction(
                    manager = manager,
                    pollAction = compositeButtonAction,
                    conversationId = conversationId
                )
            }
            is ShowResultsAction -> {
                pollService.showResultsAction(
                    manager = manager,
                    showResultsAction = compositeButtonAction,
                    conversationId = conversationId
                )
            }
        }
    }

    /**
     * Makes app interactive by executing user-issued commands.
     */
    suspend fun handleText(
        manager: WireApplicationManager,
        usersInput: UsersInput
    ) {
        val conversationId = usersInput.conversationId
        val trimmed = usersInput.text
            .trim().replace("\\s+".toRegex(), " ")
            .lowercase()

        when {
            // send version when asked
            trimmed == "/poll version" -> {
                userCommunicationService.sendVersion(manager, conversationId)
            }
            // send version when asked
            trimmed == "/poll help" -> {
                userCommunicationService.sendHelp(manager, conversationId)
            }
            // poll request
            trimmed.startsWith("/poll") ->
                pollService.createPoll(manager, usersInput)
            // Easter egg, good app is good
            trimmed == "good app" -> {
                userCommunicationService.goodApp(manager, conversationId)
            }
            else -> logger.debug("Ignoring the message, unrecognized command.")
        }
    }
}
