package com.wire.apps.polls.dto

import com.wire.integrations.jvm.model.QualifiedId
import com.wire.integrations.jvm.model.WireMessage

/**
 * Represents poll vote from the user.
 */
data class PollAction(
    val pollId: String,
    val optionId: Int,
    val userId: QualifiedId
) {
    companion object {
        fun fromWire(wireMessage: WireMessage.ButtonAction): PollAction =
            PollAction(
                pollId = wireMessage.referencedMessageId,
                optionId = wireMessage.buttonId.toInt(),
                userId = wireMessage.sender
            )
    }
}
