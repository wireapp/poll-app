package com.wire.bots.polls.dto.bot

import com.wire.integrations.jvm.model.WireMessage

internal data class NewPoll(
    val text: Text,
    val poll: WireMessage.Composite,
    override val type: String = "poll"
) : BotMessage {
    internal data class Poll(
        val id: String,
        val buttons: List<WireMessage.Composite.Button>,
        val type: String = "create"
    )
}
