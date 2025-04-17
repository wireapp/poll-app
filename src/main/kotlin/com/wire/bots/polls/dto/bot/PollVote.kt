package com.wire.bots.polls.dto.bot

internal data class PollVote(
    val poll: Poll,
    override val type: String = "poll"
) : BotMessage {
    internal data class Poll(
        val id: String,
        val offset: Int,
        val userId: String,
        val type: String = "confirmation"
    )
}
