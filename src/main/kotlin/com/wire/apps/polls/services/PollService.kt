package com.wire.apps.polls.services

import com.wire.apps.polls.dao.PollRepository
import com.wire.apps.polls.dto.UsersInput
import com.wire.apps.polls.dto.app.confirmVote
import com.wire.apps.polls.dto.app.newPoll
import com.wire.apps.polls.parser.PollFactory
import com.wire.integrations.jvm.model.QualifiedId
import com.wire.integrations.jvm.model.WireMessage
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

    /**
     * Create poll.
     */
    suspend fun createPoll(
        manager: WireApplicationManager,
        conversationId: QualifiedId,
        usersInput: UsersInput
    ) {
        val poll = factory
            .forUserInput(usersInput)
            .whenNull {
                logger.warn { "It was not possible to create poll." }
                pollNotParsedFallback(manager, conversationId, usersInput)
            } ?: return

        val message = newPoll(
            conversationId = conversationId,
            body = poll.question.body,
            buttons = poll.options
        )

        val pollId = repository.savePoll(
            poll,
            pollId = message.id.toString(),
            userId = usersInput.userId?.id.toString(),
            userDomain = usersInput.userId?.domain.toString(),
            conversationId = conversationId.id.toString()
        )
        logger.info { "Poll successfully created with id: $pollId" }

        proxySenderService.send(manager, message, conversationId)
    }

    private suspend fun pollNotParsedFallback(
        manager: WireApplicationManager,
        conversationId: QualifiedId,
        usersInput: UsersInput
    ) {
        usersInput.input.startsWith("/poll").whenTrue {
            logger.info { "Command started with /poll, sending usage to user." }
            userCommunicationService.reactionToWrongCommand(manager, conversationId)
        }
    }

    /**
     * Record that the user voted.
     */
    suspend fun pollAction(
        manager: WireApplicationManager,
        pollAction: WireMessage.ButtonAction,
        conversationId: QualifiedId
    ) {
        logger.info { "User voted" }
        repository.vote(pollAction)
        logger.info { "Vote registered." }

        val message = confirmVote(
            pollId = pollAction.referencedMessageId,
            conversationId,
            offset = pollAction.buttonId.toInt()
        )
        proxySenderService.send(manager, message, conversationId)
        sendStatsIfAllVoted(manager, pollAction.referencedMessageId, conversationId)
    }

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
            sendStats(manager, pollId, conversationId, conversationMembersCount)
        } else {
            logger.info {
                "Users voted: $votedSize, members of conversation: $conversationMembersCount"
            }
        }
    }

    /**
     * Sends statistics about the poll to the proxy.
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
            .formatStats(pollId, conversationId, conversationMembersCount)
            .whenNull { logger.warn { "It was not possible to format stats for poll $pollId" } }
            ?: return

        proxySenderService.send(manager, stats, conversationId)
    }

    /**
     * Sends stats for latest poll.
     */
    suspend fun sendStatsForLatest(
        manager: WireApplicationManager,
        conversationId: QualifiedId
    ) {
        logger.debug { "Sending latest stats" }

        val latest = repository.getLatestForConversation(conversationId).whenNull {
            logger.info { "No polls found for conversation $conversationId" }
        } ?: return
        // todo send message to user that no polls were created
        sendStats(manager, latest, conversationId)
    }
}
