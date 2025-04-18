package com.wire.bots.polls.dto.bot

import com.wire.bots.polls.dto.common.Mention
import com.wire.bots.polls.dto.common.Text

/**
 * Creates message for poll.
 */
fun newPoll(
    id: String,
    body: String,
    buttons: List<String>,
    mentions: List<Mention> = emptyList()
): BotMessage =
    NewPoll(
        text = Text(body, mentions),
        poll = NewPoll.Poll(
            id = id,
            buttons = buttons
        )
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
