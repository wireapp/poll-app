package com.wire.apps.polls.dto.app

/**
 * Common interface for the messages sent to proxy from the bot.
 */
interface BotMessage {
    /**
     * Mandatory type of the message.
     */
    val type: String
}
