package com.wire.apps.polls.services

import com.wire.apps.polls.dao.PollRepository
import com.wire.apps.polls.dto.PollAction.VoteAction
import com.wire.apps.polls.dto.PollAction.ShowResultsAction
import com.wire.apps.polls.dto.PollVoteCountProgress
import com.wire.apps.polls.dto.UsersInput
import com.wire.apps.polls.parser.PollFactory
import com.wire.apps.polls.services.UserCommunicationService.FallbackMessageType.WRONG_COMMAND
import com.wire.sdk.model.QualifiedId
import com.wire.sdk.service.WireApplicationManager
import mu.KLogging
import pw.forst.katlib.whenNull
import pw.forst.katlib.whenTrue

/**
 * Service handling the polls. It communicates with the user via [userCommunicationService].
 */
class PollService(
    private val factory: PollFactory,
    private val pollRepository: PollRepository,
    private val conversationService: ConversationService,
    private val userCommunicationService: UserCommunicationService
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

        val pollId = pollRepository.savePoll(
            poll = poll,
            pollId = messageId,
            userId = usersInput.sender.id.toString(),
            userDomain = usersInput.sender.domain,
            conversationId = conversationId.id.toString()
        )
        refreshOverview(
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
     * Registers vote in database and triggers post vote update of overview message
     */
    suspend fun processVoteAction(
        manager: WireApplicationManager,
        voteAction: VoteAction,
        conversationId: QualifiedId
    ) {
        val pollId = voteAction.pollId

        logger.info {
            "User ${voteAction.userId} voted in poll $pollId in conversation $conversationId"
        }
        pollRepository.saveVote(voteAction)
        onPollActionProcessed(
            manager = manager,
            pollId = pollId,
            conversationId = conversationId
        )
    }

    suspend fun processShowResultsAction(
        manager: WireApplicationManager,
        showResultsAction: ShowResultsAction,
        conversationId: QualifiedId
    ) {
        val pollId = showResultsAction.pollId

        logger.info {
            "User ${showResultsAction.userId} requested results for $pollId " +
                "in conversation $conversationId"
        }
        pollRepository.setResultVisibilityToTrue(pollId)
        onPollActionProcessed(
            manager = manager,
            pollId = pollId,
            conversationId = conversationId
        )
    }

    /**
     * Voting triggers update of poll overview message,
     * and if everyone voted, we send the stats.
     */
    private suspend fun onPollActionProcessed(
        manager: WireApplicationManager,
        pollId: String,
        conversationId: QualifiedId
    ) {
        val conversationMembersCount = conversationService
            .getNumberOfConversationMembers(manager, conversationId)
        val votedSize = pollRepository.votingUsers(pollId).size
        val voteCountProgress = PollVoteCountProgress(votedSize, conversationMembersCount)

        logger.info {
            "Users voted: ${voteCountProgress.totalVoteCount}, " +
                "members of conversation: ${voteCountProgress.totalMembers} in poll $pollId"
        }

        if (voteCountProgress.everyoneVoted()) {
            logger.info {
                "All users voted, sending statistics to the conversation $conversationId"
            }
            pollRepository.setResultVisibilityToTrue(pollId)
        }
        refreshOverview(
            manager = manager,
            pollId = pollId,
            conversationId = conversationId,
            voteCountProgress = voteCountProgress
        )
    }

    /**
     * Displays visual representation with percentage of user who cast a votes to conversation size.
     */
    internal suspend fun refreshOverview(
        manager: WireApplicationManager,
        pollId: String,
        conversationId: QualifiedId,
        voteCountProgress: PollVoteCountProgress = PollVoteCountProgress.initial()
    ) {
        val overviewMessageId = pollRepository.getOverviewMessageId(pollId)

        val newOverviewMessageId = when {
            overviewMessageId == null -> {
                userCommunicationService.sendInitialPollOverview(
                    manager = manager,
                    conversationId = conversationId
                )
            }
            pollRepository.isResultVisible(pollId) -> {
                userCommunicationService.updatePollResults(
                    manager = manager,
                    conversationId = conversationId,
                    overviewMessageId = overviewMessageId,
                    voteCountProgress = voteCountProgress,
                    pollId = pollId
                )
            }
            else -> {
                userCommunicationService.updatePollProgressBar(
                    manager = manager,
                    conversationId = conversationId,
                    overviewMessageId = overviewMessageId,
                    voteCountProgress = voteCountProgress.display()
                )
            }
        }
        pollRepository.setOverviewMessageId(pollId, newOverviewMessageId)
    }
}
