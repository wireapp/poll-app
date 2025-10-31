package com.wire.apps.polls.services

import com.wire.sdk.exception.WireException
import com.wire.sdk.model.WireMessage
import com.wire.sdk.service.WireApplicationManager
import mu.KLogging

/**
 * Service responsible for sending requests to the proxy service.
 */
class ProxySenderService {
    private companion object : KLogging()

    /**
     * Respond to the event issued by user.
     */
    suspend fun send(
        manager: WireApplicationManager,
        message: WireMessage
    ) {
        try {
            manager.sendMessageSuspending(message)
        } catch (e: WireException.EntityNotFound) {
            logger.error {
                "It was not possible to send a message in ${message.conversationId}. ${e.message}"
            }
        }
    }
}
