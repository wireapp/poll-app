package com.wire.apps.polls.services

import com.wire.apps.polls.dao.PollRepository
import com.wire.apps.polls.dto.PollAction
import com.wire.apps.polls.dto.UsersInput
import com.wire.apps.polls.dto.common.Text
import com.wire.apps.polls.parser.PollFactory
import com.wire.integrations.jvm.model.QualifiedId
import com.wire.integrations.jvm.service.WireApplicationManager
import mu.KLogging
import pw.forst.katlib.whenNull
import pw.forst.katlib.whenTrue

/**
 * Service handling the polls.
 */
class PollService(
    private val factory: PollFactory,
    private val repository: PollRepository,
    private val conversationService: ConversationService,
    private val userCommunicationService: UserCommunicationService,
    private val statsFormattingService: StatsFormattingService
) {
    private companion object : KLogging()

    suspend fun createPoll(
        manager: WireApplicationManager,
        usersInput: UsersInput
    ) {
        val conversationId = usersInput.conversationId
        val poll = factory
            .forUserInput(usersInput)
            .whenNull {
                logger.warn { "It was not possible to create poll." }
                pollNotParsedFallback(
                    manager = manager,
                    conversationId = conversationId,
                    usersInput = usersInput
                )
            } ?: return

        val messageId = userCommunicationService.sendPoll(
            manager = manager,
            conversationId = conversationId,
            poll = poll
        )

        val pollId = repository.savePoll(
            poll = poll,
            pollId = messageId,
            userId = usersInput.sender.id.toString(),
            userDomain = usersInput.sender.domain,
            conversationId = conversationId.id.toString()
        )
        logger.info { "Poll successfully created with id: $pollId" }
    }

    private suspend fun pollNotParsedFallback(
        manager: WireApplicationManager,
        conversationId: QualifiedId,
        usersInput: UsersInput
    ) {
        usersInput.text.startsWith("/poll").whenTrue {
            logger.info { "Command started with /poll, sending usage to user." }
            userCommunicationService.reactionToWrongCommand(
                manager = manager,
                conversationId = conversationId,
                message = "I couldn't recognize your command."
            )
        }
    }

    /**
     * Confirms to the user that their vote has been successfully registered.
     */
    suspend fun pollAction(
        manager: WireApplicationManager,
        pollAction: PollAction,
        conversationId: QualifiedId
    ) {
        logger.info { "User voted" }
        repository.vote(pollAction)
        logger.info { "Vote registered." }

        userCommunicationService.sendButtonConfirmation(
            manager = manager,
            pollAction = pollAction,
            conversationId = conversationId
        )
        sendStatsOrUpdate(
            manager = manager,
            pollId = pollAction.pollId,
            conversationId = conversationId
        )
    }

    /**
     * Provides users with immediate confirmation that voting is complete.
     */
    internal suspend fun sendStatsOrUpdate(
        manager: WireApplicationManager,
        pollId: String,
        conversationId: QualifiedId
    ) {
        val stats = generatePollStats(
            manager = manager,
            conversationId = conversationId,
            pollId = pollId
        ) ?: return userCommunicationService.reactionToWrongCommand(
            manager = manager,
            conversationId = conversationId,
            message = "No data for poll. Please create a new one."
        )
        val statsMessageId = repository.getStatsMessage(pollId)

        if (statsMessageId == null) {
            sendStats(
                manager = manager,
                pollId = pollId,
                stats = stats,
                conversationId = conversationId
            )
        } else {
            updateStats(
                manager = manager,
                pollId = pollId,
                stats = stats,
                conversationId = conversationId,
                statsMessageId = statsMessageId
            )
        }
    }

    private suspend fun generatePollStats(
        manager: WireApplicationManager,
        conversationId: QualifiedId,
        pollId: String
    ): Text? {
        val conversationMembersCount = conversationService
            .getNumberOfConversationMembers(manager, conversationId)
        val votedSize = repository.votingUsers(pollId).size

        logger.info {
            "Users voted: $votedSize, members of conversation: $conversationMembersCount"
        }

        return statsFormattingService
            .formatStats(
                pollId = pollId,
                conversationMembers = conversationMembersCount
            )
    }

    private suspend fun sendStats(
        manager: WireApplicationManager,
        pollId: String,
        stats: Text,
        conversationId: QualifiedId
    ) {
        logger.debug { "Sending stats for poll $pollId" }
        val statsMessageId = userCommunicationService.sendStats(
            manager = manager,
            conversationId = conversationId,
            text = stats
        )
        repository.setStatsMessage(pollId, statsMessageId)
    }

    private suspend fun updateStats(
        manager: WireApplicationManager,
        pollId: String,
        stats: Text,
        conversationId: QualifiedId,
        statsMessageId: String
    ) {
        logger.debug { "Updating stats for poll $pollId" }
        val newStatsMessageId = userCommunicationService.sendUpdatedStats(
            manager = manager,
            conversationId = conversationId,
            text = stats,
            statsMessageId = statsMessageId
        )
        repository.setStatsMessage(pollId, newStatsMessageId)
    }
}
