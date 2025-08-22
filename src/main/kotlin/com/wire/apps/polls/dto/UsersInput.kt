package com.wire.apps.polls.dto

import com.wire.apps.polls.dto.common.Mention
import com.wire.integrations.jvm.model.QualifiedId
import com.wire.integrations.jvm.model.WireMessage

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
    override fun toString(): String = "User $sender sent message in conversation $conversationId"

    companion object {
        fun fromWire(wireMessage: WireMessage.Text): UsersInput {
            val mentions = Mention.fromWireList(wireMessage.mentions)

            return UsersInput(
                sender = wireMessage.sender,
                conversationId = wireMessage.conversationId,
                text = wireMessage.text,
                mentions = mentions
            )
        }
    }
}
