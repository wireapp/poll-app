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

fun newVoteCount(conversationId: QualifiedId) =
    WireMessage.Text.create(
        conversationId = conversationId,
        text = PollVoteCountProgress.new()
    )

fun updateVoteCount(
    conversationId: QualifiedId,
    voteCountMessageId: String,
    voteCountProgress: PollVoteCountProgress
) = WireMessage.TextEdited.create(
    replacingMessageId = UUID.fromString(voteCountMessageId),
    conversationId = conversationId,
    text = voteCountProgress.display()
)

/**
 * Creates stats (result of the poll) message.
 */
fun statsMessage(
    conversationId: QualifiedId,
    text: Text
) = WireMessage.Text.create(
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
