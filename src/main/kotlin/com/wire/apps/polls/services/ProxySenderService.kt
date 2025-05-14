package com.wire.apps.polls.services

import com.wire.integrations.jvm.exception.WireException
import com.wire.integrations.jvm.model.WireMessage
import com.wire.integrations.jvm.service.WireApplicationManager
import mu.KLogging
import pw.forst.katlib.createJson

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
        logger.debug { "Sending: ${createJson(message)}" }
        try {
            manager.sendMessageSuspending(message)
        } catch (e: WireException.EntityNotFound) {
            logger.error { "It was not possible to send a message. ${e.message}" }
        }
    }
}
