package com.wire.apps.polls.services

import com.wire.apps.polls.dto.PollAction
import com.wire.apps.polls.dto.PollAction.VoteAction
import com.wire.apps.polls.dto.PollAction.ShowResultsAction
import com.wire.apps.polls.dto.UsersInput
import com.wire.apps.polls.setup.metrics.UsageMetrics
import com.wire.sdk.model.QualifiedId
import com.wire.sdk.service.WireApplicationManager
import mu.KLogging

/**
 * Connect conversation-based events to app functionality
 */
class MessagesHandlingService(
    private val pollService: PollService,
    private val userCommunicationService: UserCommunicationService,
    private val usageMetrics: UsageMetrics
) {
    private companion object : KLogging()

    /**
     * Welcomes the user by listing available commands.
     */
    suspend fun handleAppAddedToConversation(
        manager: WireApplicationManager,
        conversationId: QualifiedId
    ) {
        usageMetrics.onAppAddedToConversation()
        userCommunicationService.sayHello(manager, conversationId)
    }

    /**
     * Records the user's vote based on the selected button.
     */
    suspend fun handlePollAction(
        manager: WireApplicationManager,
        pollAction: PollAction,
        conversationId: QualifiedId
    ) {
        when (pollAction) {
            is VoteAction -> {
                pollService.processVoteAction(
                    manager = manager,
                    voteAction = pollAction,
                    conversationId = conversationId
                )
            }
            is ShowResultsAction -> {
                pollService.processShowResultsAction(
                    manager = manager,
                    showResultsAction = pollAction,
                    conversationId = conversationId
                )
            }
        }
    }

    /**
     * Makes app interactive by executing user-issued commands.
     */
    suspend fun handleUserCommand(
        manager: WireApplicationManager,
        usersInput: UsersInput
    ) {
        val conversationId = usersInput.conversationId
        val trimmed = usersInput.text
            .trim()
            .replace("\\s+".toRegex(), " ")
            .lowercase()

        when {
            // send version when asked
            trimmed == "/poll version" -> {
                userCommunicationService.sendVersion(manager, conversationId)
            }
            // send version when asked
            trimmed == "/poll help" -> {
                usageMetrics.onHelpCommand()
                userCommunicationService.sendHelp(manager, conversationId)
            }
            // poll creation request
            trimmed.startsWith("/poll") -> {
                usageMetrics.onCreatePollCommand()
                pollService.createPoll(manager, usersInput)
            }
            // Easter egg, good app is good
            trimmed == "good app" -> {
                userCommunicationService.goodApp(manager, conversationId)
            }
            else -> logger.debug("Ignoring the message, unrecognized command.")
        }
    }
}
