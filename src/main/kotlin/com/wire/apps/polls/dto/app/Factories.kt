package com.wire.apps.polls.dto.app

import com.wire.integrations.jvm.model.QualifiedId
import com.wire.integrations.jvm.model.WireMessage
import java.util.UUID

/**
 * Creates message for poll.
 */
fun newPoll(
    conversationId: QualifiedId,
    body: String,
    buttons: List<WireMessage.Composite.Button>
): WireMessage.Composite =
    WireMessage.Composite.create(
        conversationId = conversationId,
        text = body,
        buttonList = buttons
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
 * Creates message which greets the users in the conversation.
 */
fun greeting(
    conversationId: QualifiedId,
    text: String
): WireMessage.Text = WireMessage.Text.create(conversationId, text)

/**
 * Creates stats (result of the poll) message.
 */
fun statsMessage(
    conversationId: QualifiedId,
    text: String
): WireMessage.Text = WireMessage.Text.create(conversationId, text)

/**
 * Creates message notifying user about wrongly used command.
 */
fun fallBackMessage(
    conversationId: QualifiedId,
    text: String
): WireMessage.Text = WireMessage.Text.create(conversationId, text)

/**
 * Creates good app message.
 */
fun goodAppMessage(
    conversationId: QualifiedId,
    text: String
): WireMessage.Text = WireMessage.Text.create(conversationId, text)

/**
 * Creates version message.
 */
fun versionMessage(
    conversationId: QualifiedId,
    text: String
): WireMessage.Text = WireMessage.Text.create(conversationId, text)

/**
 * Creates message with help.
 */
fun helpMessage(
    conversationId: QualifiedId,
    text: String
): WireMessage.Text = WireMessage.Text.create(conversationId, text)
