package com.wire.apps.polls.services

import com.wire.apps.polls.dto.PollDto
import com.wire.apps.polls.dto.PollVoteCountProgress
import com.wire.apps.polls.dto.updatePollOverviewResults
import com.wire.apps.polls.dto.createPollOverview
import com.wire.apps.polls.dto.newPoll
import com.wire.apps.polls.dto.textMessage
import com.wire.apps.polls.dto.updatePollOverviewProgressBar
import com.wire.sdk.model.QualifiedId
import com.wire.sdk.model.WireMessage
import com.wire.sdk.service.WireApplicationManager
import mu.KLogging
import pw.forst.katlib.whenNull

/**
 * Service used for handling user commands.
 */
class UserCommunicationService(
    private val proxySenderService: ProxySenderService,
    private val statsFormattingService: StatsFormattingService,
    private val version: String
) {
    private companion object : KLogging() {
        private const val WELCOME_TEXT =
            "👋 Hi, I'm the Poll App. Thanks for adding me to the conversation.\n" +
                "You can use me to create polls directly in Wire.\n" +
                "I'm here to help make everyday work a little easier.\n"
        const val USAGE = "To create poll please text: " +
            "`/poll \"Question\" \"Option 1\" \"Option 2\"`. To display usage write `/poll help`"

        const val WELCOME_MESSAGE = WELCOME_TEXT + USAGE
    }

    /**
     * Introduce utility of an app to the user.
     */
    suspend fun sayHello(
        manager: WireApplicationManager,
        conversationId: QualifiedId
    ) {
        manager.send(textMessage(conversationId, WELCOME_MESSAGE))
        logger.info("App sent a welcome message. conversationId: $conversationId")
    }

    /**
     * Fallback message in case of user providing invalid command.
     */
    suspend fun sendFallbackMessage(
        manager: WireApplicationManager,
        conversationId: QualifiedId,
        messageType: FallbackMessageType
    ) {
        val message = when (messageType) {
            FallbackMessageType.WRONG_COMMAND -> "I couldn't recognize your command. $USAGE"
            FallbackMessageType.MISSING_DATA -> "No data for poll. Please create a new one. $USAGE"
        }
        manager.send(textMessage(conversationId, message))
    }

    enum class FallbackMessageType {
        WRONG_COMMAND,
        MISSING_DATA
    }

    /**
     * Remind user of possible commands.
     */
    suspend fun sendHelp(
        manager: WireApplicationManager,
        conversationId: QualifiedId
    ) {
        manager.send(textMessage(conversationId, USAGE))
    }

    /**
     * Show gratitude to the app.
     */
    suspend fun goodApp(
        manager: WireApplicationManager,
        conversationId: QualifiedId
    ) {
        manager.send(textMessage(conversationId, "\uD83D\uDE07"))
    }

    /**
     * Informs the user whether they are interacting with a development or deployed version.
     */
    suspend fun sendVersion(
        manager: WireApplicationManager,
        conversationId: QualifiedId
    ) {
        manager.send(textMessage(conversationId, "My version is: *$version*"))
    }

    suspend fun sendPoll(
        manager: WireApplicationManager,
        conversationId: QualifiedId,
        poll: PollDto
    ): String {
        val message = newPoll(
            conversationId = conversationId,
            body = poll.question.data,
            buttons = poll.options,
            mentions = poll.question.mentions
        )
        manager.send(message)

        return message.id.toString()
    }

    suspend fun sendInitialPollOverview(
        manager: WireApplicationManager,
        conversationId: QualifiedId
    ): String {
        val wireMessage = createPollOverview(conversationId)

        logger.debug {
            "Sending initial Poll overview for conversation $conversationId"
        }
        manager.send(wireMessage)

        return wireMessage.id.toString()
    }

    suspend fun updatePollProgressBar(
        manager: WireApplicationManager,
        conversationId: QualifiedId,
        overviewMessageId: String,
        voteCountProgress: String
    ): String {
        val wireMessage = updatePollOverviewProgressBar(
            conversationId = conversationId,
            overviewMessageId = overviewMessageId,
            voteCountProgress = voteCountProgress
        )

        logger.debug {
            "Updating progress bar of Poll overview for conversation $conversationId"
        }
        manager.send(wireMessage)

        return wireMessage.id.toString()
    }

    suspend fun updatePollResults(
        manager: WireApplicationManager,
        conversationId: QualifiedId,
        overviewMessageId: String,
        voteCountProgress: PollVoteCountProgress,
        pollId: String
    ): String? {
        val stats = statsFormattingService
            .formatStats(
                pollId = pollId,
                conversationMembers = voteCountProgress.totalMembers
            ).whenNull {
                logger.error { "It was not possible to send stats for poll $pollId" }
                sendFallbackMessage(
                    manager = manager,
                    conversationId = conversationId,
                    messageType = FallbackMessageType.MISSING_DATA
                )
            } ?: return null
        val wireMessage = updatePollOverviewResults(
            conversationId = conversationId,
            overviewMessageId = overviewMessageId,
            stats = stats,
            voteCountProgress = voteCountProgress.display()
        )

        logger.debug {
            "Sending stats for poll $pollId " +
                "Conversation members: ${voteCountProgress.totalMembers}"
        }
        manager.send(wireMessage)

        return wireMessage.id.toString()
    }

    private suspend fun WireApplicationManager.send(message: WireMessage) {
        proxySenderService.send(
            manager = this,
            message = message
        )
    }
}
