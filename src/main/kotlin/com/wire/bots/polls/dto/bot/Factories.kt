package com.wire.bots.polls.dto.bot

import com.wire.bots.polls.dto.common.Mention
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
    text: String,
    mentions: List<Mention> = emptyList()
): BotMessage = text(text, mentions)

/**
 * Creates stats (result of the poll) message.
 */
fun statsMessage(
    text: String,
    mentions: List<Mention> = emptyList()
): BotMessage = text(text, mentions)

/**
 * Creates message notifying user about wrongly used command.
 */
fun fallBackMessage(
    text: String,
    mentions: List<Mention> = emptyList()
): BotMessage = text(text, mentions)

/**
 * Creates good bot message.
 */
fun goodBotMessage(
    text: String,
    mentions: List<Mention> = emptyList()
): BotMessage = text(text, mentions)

/**
 * Creates version message.
 */
fun versionMessage(
    text: String,
    mentions: List<Mention> = emptyList()
): BotMessage = text(text, mentions)

/**
 * Creates message with help.
 */
fun helpMessage(
    text: String,
    mentions: List<Mention> = emptyList()
): BotMessage = text(text, mentions)

/**
 * Creates text message.
 */
private fun text(
    text: String,
    mentions: List<Mention>
): BotMessage =
    TextMessage(
        text = Text(
            data = text,
            mentions = mentions
        )
    )
