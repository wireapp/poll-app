package com.wire.apps.polls.dto

import com.wire.apps.polls.dto.common.Mention
import com.wire.integrations.jvm.model.QualifiedId
import com.wire.integrations.jvm.model.WireMessage
import io.ktor.features.BadRequestException

/**
 * Wrapper for the text received by this app. Should be used as a container for all user texts in the app.
 *
 * This is in order not to log sensitive text to the log.
 */
data class UsersInput(
    /**
     * Id of the user who wrote this.
     */
    val sender: QualifiedId,
    /**
     * Id of the conversation where user wrote message.
     */
    val conversationId: QualifiedId,
    /**
     * User's text, not logged.
     */
    val text: String,
    /**
     * Mentions
     */
    val mentions: List<Mention>
) {
    // TODO modify this in the future - because we do not want to print decrypted users text to the log
    override fun toString(): String = "User: $sender wrote $text"

    companion object {
        fun fromWire(wireMessage: WireMessage.Text): UsersInput? {
            val sender = wireMessage.sender
            sender ?: throw BadRequestException("Sender must be set for text messages.")
            val conversationId = wireMessage.conversationId
            val text = wireMessage.text ?: return null
            val mentions = Mention.fromWireList(wireMessage.mentions)

            return UsersInput(
                sender = sender,
                conversationId = conversationId,
                text = text,
                mentions = mentions
            )
        }
    }
}
