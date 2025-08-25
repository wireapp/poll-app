package com.wire.apps.polls.dto

import com.wire.apps.polls.dto.Option.Companion.OPTION_BUTTON_PREFIX
import com.wire.apps.polls.dto.PollOverviewDto.Companion.RESULTS_BUTTON_ID
import com.wire.integrations.jvm.model.QualifiedId
import com.wire.integrations.jvm.model.WireMessage

sealed interface ButtonAction {
    companion object {
        fun fromWire(wireMessage: WireMessage.ButtonAction): ButtonAction? {
            return when {
                wireMessage.buttonId == RESULTS_BUTTON_ID -> {
                    ShowResultsAction(
                        messageId = wireMessage.referencedMessageId,
                        userId = wireMessage.sender
                    )
                }

                wireMessage.buttonId.startsWith(OPTION_BUTTON_PREFIX) -> {
                    val optionId = wireMessage.buttonId.removePrefix(OPTION_BUTTON_PREFIX).toInt()

                    PollAction(
                        pollId = wireMessage.referencedMessageId,
                        optionId = optionId,
                        userId = wireMessage.sender
                    )
                }

                else -> null
            }
        }
    }

    data class ShowResultsAction(
        val messageId: String,
        val userId: QualifiedId
    ) : ButtonAction

    /**
     * Represents poll vote from the user.
     */
    data class PollAction(
        val pollId: String,
        val optionId: Int,
        val userId: QualifiedId
    ) : ButtonAction
}
