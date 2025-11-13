package com.wire.apps.polls.dto

import com.wire.apps.polls.dao.PollRepository
import com.wire.apps.polls.utils.OPTION_BUTTON_PREFIX
import com.wire.apps.polls.utils.RESULTS_BUTTON_ID
import com.wire.sdk.model.WireMessage
import mu.KLogging
import pw.forst.katlib.whenNull

/**
 * To determine if a generic event is a button click on a specific Poll App message type,
 * the respective button identifiers are assigned at creation:
 * [RESULTS_BUTTON_ID] and [OPTION_BUTTON_PREFIX].
 */
class PollActionMapper(
    private val pollRepository: PollRepository
) {
    private companion object : KLogging()

    suspend fun fromButtonAction(buttonAction: WireMessage.ButtonAction): PollAction? =
        when {
            buttonAction.isShowResults() -> mapToShowResultsAction(buttonAction)
            buttonAction.isVoteAction() -> mapToVoteAction(buttonAction)
            else -> {
                logger.debug {
                    "Unknown ButtonAction. conversationId: ${buttonAction.conversationId}, " +
                        "referencedMessageId: ${buttonAction.referencedMessageId}," +
                        "sender: ${buttonAction.sender}"
                }
                null
            }
        }

    private suspend fun mapToShowResultsAction(
        buttonAction: WireMessage.ButtonAction
    ): PollAction.ShowResultsAction? {
        val pollId = pollRepository
            .getPollId(buttonAction.referencedMessageId)
            .whenNull {
                logger.warn {
                    "Show results button was already pressed " +
                        "by another user in conversation ${buttonAction.conversationId}"
                }
            } ?: return null

        return PollAction.ShowResultsAction(
            pollId = pollId,
            userId = buttonAction.sender
        )
    }

    private fun mapToVoteAction(buttonAction: WireMessage.ButtonAction): PollAction.VoteAction {
        val optionIndex = buttonAction.buttonId.removePrefix(OPTION_BUTTON_PREFIX).toInt()

        return PollAction.VoteAction(
            pollId = buttonAction.referencedMessageId,
            optionIndex = optionIndex,
            userId = buttonAction.sender
        )
    }

    private suspend fun WireMessage.ButtonAction.isShowResults(): Boolean =
        buttonId == RESULTS_BUTTON_ID &&
            pollRepository.isPollMessage(referencedMessageId)

    private suspend fun WireMessage.ButtonAction.isVoteAction(): Boolean =
        buttonId.startsWith(OPTION_BUTTON_PREFIX) &&
            pollRepository.isPollMessage(referencedMessageId)
}
