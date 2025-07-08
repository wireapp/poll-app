package com.wire.apps.polls.services

import com.wire.apps.polls.dao.PollRepository
import com.wire.apps.polls.dto.PollAction
import com.wire.apps.polls.dto.PollVoteCountProgress
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
                logger.warn {
                    "It was not possible to create poll " +
                        "in conversation ${usersInput.conversationId}"
                }
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
        sendParticipation(
            manager = manager,
            conversationId = conversationId,
            pollId = pollId
        )

        logger.info { "Poll successfully created with id: $pollId" }
    }

    private suspend fun pollNotParsedFallback(
        manager: WireApplicationManager,
        conversationId: QualifiedId,
        usersInput: UsersInput
    ) {
        usersInput.text.startsWith("/poll").whenTrue {
            logger.info {
                "Invalid command started with /poll, sending usage to conversation $conversationId"
            }
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
        logger.info { "User voted ${pollAction.userId} in poll ${pollAction.pollId}" }
        repository.vote(pollAction)

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
        val voteCountProgress = PollVoteCountProgress(votedSize, conversationMembersCount)

        logger.info { "${voteCountProgress.logInfo()} in poll $pollId" }

        if (voteCountProgress.everyoneVoted()) {
            logger.info {
                "All users voted, sending statistics to the conversation $conversationId"
            }
            sendStats(
                manager = manager,
                pollId = pollId,
                conversationId = conversationId,
                conversationMembers = voteCountProgress.totalMembers
            )
        }
        sendParticipation(
            manager = manager,
            pollId = pollId,
            conversationId = conversationId,
            voteCountProgress = voteCountProgress
        )
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
        logger.debug {
            "Sending stats for poll $pollId " +
                "Conversation members: $conversationMembers"
        }
        val stats = statsFormattingService
            .formatStats(
                pollId = pollId,
                conversationMembers = conversationMembers
            ).whenNull {
                logger.error { "It was not possible send stats for poll $pollId" }
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
     * Displays visual representation with percentage of user who cast a votes to conversation size.
     */
    private suspend fun sendParticipation(
        manager: WireApplicationManager,
        pollId: String,
        conversationId: QualifiedId,
        voteCountProgress: PollVoteCountProgress = PollVoteCountProgress.initial()
    ) {
        val participationMessageId = repository.getParticipationId(pollId)
        val newParticipationMessageId = userCommunicationService.sendOrUpdateParticipation(
            manager = manager,
            conversationId = conversationId,
            participationMessageId = participationMessageId,
            voteCountProgress = voteCountProgress
        )
        repository.setParticipationId(pollId, newParticipationMessageId)
    }
}
