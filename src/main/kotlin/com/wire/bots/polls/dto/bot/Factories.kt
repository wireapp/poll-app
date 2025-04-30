package com.wire.bots.polls.dto.bot

import com.wire.bots.polls.dto.common.Text
import com.wire.integrations.jvm.model.QualifiedId
import com.wire.integrations.jvm.model.WireMessage

/**
 * Creates message for poll.
 */
fun newPoll(
    conversationId: QualifiedId,
//    id: String,
    body: String,
    buttons: List<WireMessage.Composite.Button>
//    mentions: List<WireMessage.Text.Mention> = emptyList()
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
    userId: String,
    offset: Int
): BotMessage =
    PollVote(
        poll = PollVote.Poll(
            id = pollId,
            userId = userId,
            offset = offset
        )
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
 * Creates good bot message.
 */
fun goodBotMessage(
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
