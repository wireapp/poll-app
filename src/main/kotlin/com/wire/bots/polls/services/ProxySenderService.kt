package com.wire.bots.polls.services

import com.wire.integrations.jvm.exception.WireException
import com.wire.integrations.jvm.model.QualifiedId
import com.wire.integrations.jvm.model.WireMessage
import com.wire.integrations.jvm.service.WireApplicationManager
import mu.KLogging
import pw.forst.katlib.createJson

/**
 * Service responsible for sending requests to the proxy service Roman.
 */
class ProxySenderService {
    private companion object : KLogging()

    /**
     * Send given message
     */
    suspend fun send(
        manager: WireApplicationManager,
        message: WireMessage,
        conversationId: QualifiedId?
    ) {
        logger.debug { "Sending: ${createJson(message)}" }
        try {
            conversationId ?: throw NullPointerException("Missing conversation ID.")
            manager.sendMessageSuspending(
                conversationId = conversationId,
                message = message
            )
        } catch (e: WireException.EntityNotFound) {
            logger.error { "It was not possible to send a message. ${e.message}" }
        }
    }
}
