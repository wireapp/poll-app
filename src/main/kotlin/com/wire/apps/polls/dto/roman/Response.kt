package com.wire.apps.polls.dto.roman

/**
 * Respond received from the proxy to every message from the app.
 */
data class Response(
    /**
     * ID of the message app sent.
     */
    val messageId: String
)
