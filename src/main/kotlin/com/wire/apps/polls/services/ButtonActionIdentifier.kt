package com.wire.apps.polls.services

import com.wire.apps.polls.dao.PollRepository
import com.wire.apps.polls.dto.ButtonPressed
import com.wire.apps.polls.dto.Option.Companion.OPTION_BUTTON_PREFIX
import com.wire.apps.polls.dto.PollOverviewDto.Companion.RESULTS_BUTTON_ID
import com.wire.integrations.jvm.model.WireMessage
import mu.KLogging
import pw.forst.katlib.whenNull

/**
 * To determine if a generic event is a button click on a specific Poll App message type,
 * the respective button identifiers are assigned at creation:
 * [RESULTS_BUTTON_ID] and [OPTION_BUTTON_PREFIX].
 */
class ButtonActionIdentifier(private val pollRepository: PollRepository) {
    private companion object : KLogging()

    suspend fun fromWire(buttonSelected: WireMessage.ButtonAction): ButtonPressed? =
        when {
            buttonSelected.isShowResultsButton() -> mapToResultsRequest(buttonSelected)

            buttonSelected.isPollOption() -> mapToPollVote(buttonSelected)

            else -> null
        }

    private suspend fun mapToResultsRequest(
        buttonSelected: WireMessage.ButtonAction
    ): ButtonPressed.ResultsRequest? {
        val pollId = pollRepository
            .getPollMessage(buttonSelected.referencedMessageId)
            .whenNull {
                logger.warn {
                    "Show results button was already pressed " +
                        "by another user in conversation ${buttonSelected.conversationId}"
                }
            } ?: return null

        return ButtonPressed.ResultsRequest(
            pollId = pollId,
            userId = buttonSelected.sender
        )
    }

    private fun mapToPollVote(buttonSelected: WireMessage.ButtonAction): ButtonPressed.PollVote {
        val optionIndex = buttonSelected.buttonId.removePrefix(OPTION_BUTTON_PREFIX).toInt()

        return ButtonPressed.PollVote(
            pollId = buttonSelected.referencedMessageId,
            index = optionIndex,
            userId = buttonSelected.sender
        )
    }

    private suspend fun WireMessage.ButtonAction.isShowResultsButton(): Boolean =
        buttonId == RESULTS_BUTTON_ID &&
            pollRepository.isOverviewMessage(referencedMessageId)

    private suspend fun WireMessage.ButtonAction.isPollOption(): Boolean =
        buttonId.startsWith(OPTION_BUTTON_PREFIX) &&
            pollRepository.isPollMessage(referencedMessageId)
}
