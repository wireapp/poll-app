package com.wire.bots.polls.dto

/**
 * Represents poll vote from the user.
 */
data class PollAction(
    val pollId: String,
    val optionId: Int,
    val userId: String
)
