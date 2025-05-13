package com.wire.apps.polls.services

import com.wire.apps.polls.dao.PollRepository
import com.wire.apps.polls.dto.PollAction
import com.wire.apps.polls.dto.UsersInput
import com.wire.apps.polls.dto.app.confirmVote
import com.wire.apps.polls.dto.app.newPoll
import com.wire.apps.polls.parser.PollFactory
import com.wire.integrations.jvm.model.QualifiedId
import com.wire.integrations.jvm.service.WireApplicationManager
import mu.KLogging
import pw.forst.katlib.whenNull
import pw.forst.katlib.whenTrue

/**
 * Service handling the polls. It communicates with the proxy via [proxySenderService].
 */
class PollService(
    private val factory: PollFactory,
    private val proxySenderService: ProxySenderService,
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

        val message = newPoll(
            conversationId = conversationId,
            body = poll.question.body,
            buttons = poll.options
        )

        val pollId = repository.savePoll(
            poll = poll,
            pollId = message.id.toString(),
            userId = usersInput.sender.id.toString(),
            userDomain = usersInput.sender.domain,
            conversationId = conversationId.id.toString()
        )
        logger.info { "Poll successfully created with id: $pollId" }

        proxySenderService.send(
            manager = manager,
            message = message,
            conversationId = conversationId
        )
    }

    private suspend fun pollNotParsedFallback(
        manager: WireApplicationManager,
        conversationId: QualifiedId,
        usersInput: UsersInput
    ) {
        usersInput.text.startsWith("/poll").whenTrue {
            logger.info { "Command started with /poll, sending usage to user." }
            userCommunicationService.reactionToWrongCommand(manager, conversationId)
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

        val message = confirmVote(
            pollId = pollAction.pollId,
            conversationId = conversationId,
            offset = pollAction.optionId
        )
        proxySenderService.send(
            manager = manager,
            message = message,
            conversationId = conversationId
        )
        sendStatsIfAllVoted(
            manager = manager,
            pollId = pollAction.pollId,
            conversationId = conversationId
        )
    }

    /**
     * Provides users with immediate confirmation that voting is complete.
     */
    private suspend fun sendStatsIfAllVoted(
        manager: WireApplicationManager,
        pollId: String,
        conversationId: QualifiedId
    ) {
        val conversationMembersCount = conversationService
            .getNumberOfConversationMembers(manager, conversationId)

        val votedSize = repository.votingUsers(pollId).size

        if (votedSize == conversationMembersCount) {
            logger.info { "All users voted, sending statistics to the conversation." }
            sendStats(
                manager = manager,
                pollId = pollId,
                conversationId = conversationId,
                conversationMembers = conversationMembersCount
            )
        } else {
            logger.info {
                "Users voted: $votedSize, members of conversation: $conversationMembersCount"
            }
        }
    }

    /**
     * Reveal the results to the user.
     */
    suspend fun sendStats(
        manager: WireApplicationManager,
        pollId: String,
        conversationId: QualifiedId,
        conversationMembers: Int? = null
    ) {
        logger.debug { "Sending stats for poll $pollId" }
        val conversationMembersCount =
            conversationMembers ?: conversationService
                .getNumberOfConversationMembers(manager, conversationId)

        logger.debug { "Conversation members: $conversationMembersCount" }
        val stats = statsFormattingService
            .formatStats(
                pollId = pollId,
                conversationId = conversationId,
                conversationMembers = conversationMembersCount
            )
            .whenNull { logger.warn { "It was not possible to format stats for poll $pollId" } }
            ?: return

        proxySenderService.send(
            manager = manager,
            message = stats,
            conversationId = conversationId
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
        } ?: return
        // todo send message to user that no polls were created
        sendStats(
            manager = manager,
            pollId = latest,
            conversationId = conversationId
        )
    }
}
