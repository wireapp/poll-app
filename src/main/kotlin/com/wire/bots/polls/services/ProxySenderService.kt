package com.wire.bots.polls.services

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
        message: WireMessage
    ) {
        logger.debug { "Sending: ${createJson(message)}" }
        val conversationId = when (message) {
            is WireMessage.Text -> message.conversationId
            is WireMessage.Composite -> message.textContent?.conversationId
            else -> null
        }
        if (conversationId == null) return
        manager.sendMessageSuspending(
            conversationId = conversationId,
            message = message
        )
    }
}
