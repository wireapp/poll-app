package com.wire.apps.polls.dto

import com.wire.apps.polls.dto.common.Mention
import com.wire.apps.polls.dto.common.Text
import com.wire.apps.polls.dto.common.toWireMention
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
    text: Text,
    conversationId: QualifiedId
) = WireMessage.Text.create(
    conversationId = conversationId,
    text = text.data,
    mentions = text.mentions.map { it.toWireMention() }
)

fun updateStatsMessage(
    text: Text,
    originalMessageId: String,
    conversationId: QualifiedId
) = WireMessage.TextEdited.create(
    originalMessageId = UUID.fromString(originalMessageId),
    conversationId = conversationId,
    text = text.data,
    mentions = text.mentions.map { it.toWireMention() }
)

/**
 * Creates text WireMessage for non-database bot interactions
 */
fun textMessage(
    conversationId: QualifiedId,
    text: String
): WireMessage.Text = WireMessage.Text.create(conversationId, text)
