package com.wire.bots.polls.dto.bot

import com.wire.bots.polls.dto.common.Text

internal data class NewPoll(
    val text: Text,
    val poll: Poll,
    override val type: String = "poll"
) : BotMessage {
    internal data class Poll(
        val id: String,
        val buttons: List<String>,
        val type: String = "create"
    )
}
