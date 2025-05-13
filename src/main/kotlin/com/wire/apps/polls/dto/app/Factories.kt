package com.wire.apps.polls.dto.app

import com.wire.apps.polls.dto.Option
import com.wire.apps.polls.dto.common.Mention
import com.wire.apps.polls.dto.common.mapToWireButtons
import com.wire.apps.polls.dto.common.mapToWireMentions
import com.wire.integrations.jvm.model.QualifiedId
import com.wire.integrations.jvm.model.WireMessage
import java.util.UUID

/**
 * Creates message for poll.
 */
fun newPoll(
    conversationId: QualifiedId,
    body: String,
    buttons: List<Option>
): WireMessage.Composite =
    WireMessage.Composite.create(
        conversationId = conversationId,
        text = body,
        buttonList = mapToWireButtons(buttons)
    )

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
) = WireMessage.Text(
    id = UUID.randomUUID(),
    conversationId = conversationId,
    text = text,
    mentions = mapToWireMentions(mentions)
)

/**
 * Creates text WireMessage for non-database bot interactions
 */
fun textMessage(
    conversationId: QualifiedId,
    text: String
): WireMessage.Text = WireMessage.Text.create(conversationId, text)
