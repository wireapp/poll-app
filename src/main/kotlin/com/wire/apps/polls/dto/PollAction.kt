package com.wire.apps.polls.dto

import com.wire.integrations.jvm.model.QualifiedId
import com.wire.integrations.jvm.model.WireMessage
import io.ktor.features.BadRequestException

/**
 * Represents poll vote from the user.
 */
data class PollAction(
    val pollId: String,
    val optionId: Int,
    val userId: QualifiedId
) {
    companion object {
        fun fromWire(wireMessage: WireMessage.ButtonAction): PollAction {
            val sender = wireMessage.sender
            sender ?: throw BadRequestException("Sender must be set for text messages.")

            return PollAction(
                pollId = wireMessage.referencedMessageId,
                optionId = wireMessage.buttonId.toInt(),
                userId = sender
            )
        }
    }
}
