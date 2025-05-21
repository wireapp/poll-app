package com.wire.apps.polls.dto.app

import com.wire.apps.polls.dto.Option
import com.wire.apps.polls.dto.common.Mention
import com.wire.apps.polls.dto.common.toWireMention
import com.wire.apps.polls.dto.toWireButton
import com.wire.integrations.jvm.model.QualifiedId
import com.wire.integrations.jvm.model.WireMessage
import java.util.UUID

/**
 * Creates message for poll.
 */
fun newPoll(
    conversationId: QualifiedId,
    body: String,
    buttons: List<Option>,
    mentions: List<Mention>
): WireMessage.Composite {
    val text = WireMessage.Text.create(
        conversationId = conversationId,
        text = body,
        mentions = mentions.map { it.toWireMention() }
    )
    return WireMessage.Composite(
        id = UUID.randomUUID(),
        conversationId = conversationId,
        sender = QualifiedId(UUID.randomUUID(), UUID.randomUUID().toString()),
        items = listOf(text) + buttons.map { it.toWireButton() }
    )
}

/**
 * Creates message for vote confirmation.
 */
fun confirmVote(
    pollId: String,
    conversationId: QualifiedId,
    offset: Int
): WireMessage.ButtonActionConfirmation =
    WireMessage.ButtonActionConfirmation(
        id = UUID.randomUUID(),
        conversationId = conversationId,
        sender = QualifiedId(UUID.randomUUID(), UUID.randomUUID().toString()),
        referencedMessageId = pollId,
        buttonId = offset.toString()
    )

/**
 * Creates stats (result of the poll) message.
 */
fun statsMessage(
    conversationId: QualifiedId,
    text: String,
    mentions: List<Mention>
) = WireMessage.Text.create(
    conversationId = conversationId,
    text = text,
    mentions = mentions.map { it.toWireMention() }
)

/**
 * Creates text WireMessage for non-database bot interactions
 */
fun textMessage(
    conversationId: QualifiedId,
    text: String
): WireMessage.Text = WireMessage.Text.create(conversationId, text)
