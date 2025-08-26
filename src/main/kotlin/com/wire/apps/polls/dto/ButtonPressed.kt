package com.wire.apps.polls.dto

import com.wire.apps.polls.dto.Option.Companion.OPTION_BUTTON_PREFIX
import com.wire.apps.polls.dto.PollOverviewDto.Companion.RESULTS_BUTTON_ID
import com.wire.integrations.jvm.model.QualifiedId
import com.wire.integrations.jvm.model.WireMessage

/**
 * To determine if a generic event is a button click on a specific Poll App message type,
 * the respective button identifiers are assigned at creation:
 * [RESULTS_BUTTON_ID] and [OPTION_BUTTON_PREFIX].
 */
sealed interface ButtonPressed {
    companion object {
        fun fromWire(buttonSelected: WireMessage.ButtonAction): ButtonPressed? =
            when {
                buttonSelected.isShowResultsButton() -> mapToResultsRequest(buttonSelected)

                buttonSelected.isPollOption() -> mapToPollVote(buttonSelected)

                else -> null
            }

        private fun mapToResultsRequest(buttonSelected: WireMessage.ButtonAction) =
            ResultsRequest(
                overviewMessageId = buttonSelected.referencedMessageId,
                userId = buttonSelected.sender
            )

        private fun mapToPollVote(buttonSelected: WireMessage.ButtonAction): PollVote {
            val optionIndex = buttonSelected.buttonId.removePrefix(OPTION_BUTTON_PREFIX).toInt()

            return PollVote(
                pollId = buttonSelected.referencedMessageId,
                index = optionIndex,
                userId = buttonSelected.sender
            )
        }

        private fun WireMessage.ButtonAction.isShowResultsButton(): Boolean =
            buttonId == RESULTS_BUTTON_ID

        private fun WireMessage.ButtonAction.isPollOption(): Boolean =
            buttonId.startsWith(OPTION_BUTTON_PREFIX)
    }

    /**
     * Represents user clicking on "show results" button
     */
    data class ResultsRequest(
        val overviewMessageId: String,
        val userId: QualifiedId
    ) : ButtonPressed

    /**
     * Represents which option did user choose in Poll.
     */
    data class PollVote(
        val pollId: String,
        val index: Int,
        val userId: QualifiedId
    ) : ButtonPressed
}
