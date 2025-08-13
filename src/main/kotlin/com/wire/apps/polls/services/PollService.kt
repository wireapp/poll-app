package com.wire.apps.polls.services

import com.wire.apps.polls.dao.PollRepository
import com.wire.apps.polls.dto.PollAction
import com.wire.apps.polls.dto.VoteCount
import com.wire.apps.polls.dto.UsersInput
import com.wire.apps.polls.parser.PollFactory
import com.wire.apps.polls.services.UserCommunicationService.FallbackMessageType.MISSING_DATA
import com.wire.apps.polls.services.UserCommunicationService.FallbackMessageType.WRONG_COMMAND
import com.wire.integrations.jvm.model.QualifiedId
import com.wire.integrations.jvm.service.WireApplicationManager
import mu.KLogging
import pw.forst.katlib.whenNull
import pw.forst.katlib.whenTrue

/**
 * Service handling the polls. It communicates with the user via [userCommunicationService].
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
        sendParticipation(manager = manager, conversationId = conversationId, pollId = pollId)

        logger.info { "Poll successfully created with id: $pollId" }
    }

    private suspend fun pollNotParsedFallback(
        manager: WireApplicationManager,
        conversationId: QualifiedId,
        usersInput: UsersInput
    ) {
        usersInput.text.startsWith("/poll").whenTrue {
            logger.info { "Command started with /poll, sending usage to user." }
            userCommunicationService.sendFallbackMessage(
                manager = manager,
                conversationId = conversationId,
                messageType = WRONG_COMMAND
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
        afterVoteUpdate(
            manager = manager,
            pollId = pollAction.pollId,
            conversationId = conversationId
        )
    }

    /**
     * Voting triggers update of poll participation message,
     * and if everyone voted, we send the stats.
     */
    private suspend fun afterVoteUpdate(
        manager: WireApplicationManager,
        pollId: String,
        conversationId: QualifiedId
    ) {
        val conversationMembersCount = conversationService
            .getNumberOfConversationMembers(manager, conversationId)
        val votedSize = repository.votingUsers(pollId).size
        val voteCount = VoteCount(votedSize, conversationMembersCount)

        sendParticipation(
            manager = manager,
            pollId = pollId,
            conversationId = conversationId,
            voteCount = voteCount
        )
        sendStatsIfAllVoted(
            manager = manager,
            pollId = pollId,
            conversationId = conversationId,
            voteCount = voteCount
        )
    }

    /**
     * Provides users with immediate confirmation that voting is complete.
     */
    private suspend fun sendStatsIfAllVoted(
        manager: WireApplicationManager,
        pollId: String,
        conversationId: QualifiedId,
        voteCount: VoteCount
    ) {
        if (voteCount.everyoneVoted()) {
            logger.info { "All users voted, sending statistics to the conversation." }
            sendStats(
                manager = manager,
                pollId = pollId,
                conversationId = conversationId,
                conversationMembers = voteCount.totalMembers
            )
        } else {
            logger.info { voteCount.toString() }
        }
    }

    /**
     * Reveal the results to the user.
     */
    suspend fun sendStats(
        manager: WireApplicationManager,
        pollId: String,
        conversationId: QualifiedId,
        conversationMembers: Int
    ) {
        logger.debug { "Sending stats for poll $pollId" }
        logger.debug { "Conversation members: $conversationMembers" }
        val stats = statsFormattingService
            .formatStats(
                pollId = pollId,
                conversationMembers = conversationMembers
            ).whenNull {
                logger.error { "It was not possible send stats." }
                userCommunicationService.sendFallbackMessage(
                    manager = manager,
                    conversationId = conversationId,
                    messageType = MISSING_DATA
                )
            } ?: return

        userCommunicationService.sendStats(
            manager = manager,
            conversationId = conversationId,
            text = stats
        )
    }

    /**
     * Displays intermediate results while voting is ongoing,
     * and brings final results to the front once the poll is over for easy access.
     */
    suspend fun sendStatsForLatest(
        manager: WireApplicationManager,
        conversationId: QualifiedId
    ) {
        logger.debug { "Sending latest stats" }

        val latest = repository.getCurrentPoll(conversationId).whenNull {
            logger.info { "No polls found for conversation $conversationId" }
        }.orEmpty()
        val conversationSize = conversationService
            .getNumberOfConversationMembers(manager, conversationId)

        sendStats(
            manager = manager,
            pollId = latest,
            conversationId = conversationId,
            conversationMembers = conversationSize
        )
    }

    suspend fun sendParticipation(
        manager: WireApplicationManager,
        pollId: String,
        conversationId: QualifiedId,
        voteCount: VoteCount = VoteCount.initial()
    ) {
        val participationMessageId = repository.getParticipationId(pollId)
        val newParticipationMessageId = userCommunicationService.sendOrUpdateParticipation(
            manager = manager,
            conversationId = conversationId,
            participationMessageId = participationMessageId,
            voteCount = voteCount
        )
        repository.setParticipationId(pollId, newParticipationMessageId)
    }
}
